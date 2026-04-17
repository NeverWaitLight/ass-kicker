package com.github.waitlight.asskicker.config.channel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Channel 出站 JSON 序列化与第三方响应反序列化专用 {@link ObjectMapper}，
 * 与 Web 层及 Mongo 等使用的默认 ObjectMapper 隔离。
 */
@Configuration(proxyBeanMethods = false)
public class ChannelObjectMapperConfig {

    public static final String BEAN_NAME = "channelObjectMapper";

    @Bean(name = BEAN_NAME)
    public ObjectMapper channelObjectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
