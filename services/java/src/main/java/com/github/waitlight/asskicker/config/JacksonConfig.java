package com.github.waitlight.asskicker.config;

import java.io.IOException;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * 全局 Jackson 定制：
 * <ul>
 *   <li>long/Long 与字符串互转，规避 JavaScript 无法安全表示大于 {@code Number.MAX_SAFE_INTEGER} 的整数；
 *       反序列化同时接受数字与字符串，便于客户端逐步迁移。</li>
 *   <li>反序列化阶段自动去除 String 字段首尾空格，trim 后保留空串 {@code ""}（不转为 {@code null}），
 *       确保 {@code @NotBlank} 等校验语义一致。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringJsonCustomizer() {
        return longAsStringCustomizer();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer stringTrimJsonCustomizer() {
        return builder -> builder.modules(stringTrimModule());
    }

    static Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> builder.modules(longAsStringModule());
    }

    public static SimpleModule longAsStringModule() {
        SimpleModule module = new SimpleModule("LongAsString");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addDeserializer(Long.class, new FlexibleLongDeserializer());
        module.addDeserializer(Long.TYPE, new FlexibleLongDeserializer());
        return module;
    }

    private static SimpleModule stringTrimModule() {
        SimpleModule module = new SimpleModule("StringTrim");
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        return module;
    }

    private static final class FlexibleLongDeserializer extends JsonDeserializer<Long> {

        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_STRING) {
                String text = p.getText();
                if (text == null || text.isEmpty()) {
                    return null;
                }
                return Long.parseLong(text.trim());
            }
            if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
                return p.getLongValue();
            }
            if (t == JsonToken.VALUE_NULL) {
                return null;
            }
            return (Long) ctxt.handleUnexpectedToken(Long.class, p);
        }
    }

    private static final class TrimmingStringDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_STRING) {
                String text = p.getText();
                if (text == null) {
                    return null;
                }
                return text.trim();
            }
            if (t == JsonToken.VALUE_NULL) {
                return null;
            }
            return (String) ctxt.handleUnexpectedToken(String.class, p);
        }
    }
}
