package com.github.waitlight.asskicker.config.jackson;

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
 * 将 JSON 中的 {@code long}/{@code Long} 与字符串互转，避免 JavaScript 无法安全表示超过
 * {@code Number.MAX_SAFE_INTEGER} 的整数。
 * <p>
 * 反序列化同时接受 JSON 数字与字符串，便于客户端逐步迁移。
 */
@Configuration(proxyBeanMethods = false)
public class JacksonLongAsStringConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringJsonCustomizer() {
        return longAsStringCustomizer();
    }

    static Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> builder.modules(longAsStringModule());
    }

    static SimpleModule longAsStringModule() {
        SimpleModule module = new SimpleModule("LongAsString");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        module.addDeserializer(Long.class, new FlexibleLongDeserializer());
        module.addDeserializer(Long.TYPE, new FlexibleLongDeserializer());
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
}
