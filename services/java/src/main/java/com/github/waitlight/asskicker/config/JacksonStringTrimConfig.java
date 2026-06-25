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

/**
 * 全局字符串 trim 配置，在 JSON 反序列化阶段自动去除所有 String 字段的首尾空格。
 * <p>
 * trim 后保留空字符串 {@code ""}（不转为 {@code null}），确保 {@code @NotBlank} 等校验语义一致。
 */
@Configuration(proxyBeanMethods = false)
public class JacksonStringTrimConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer stringTrimJsonCustomizer() {
        return builder -> builder.modules(stringTrimModule());
    }

    private static SimpleModule stringTrimModule() {
        SimpleModule module = new SimpleModule("StringTrim");
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        return module;
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
