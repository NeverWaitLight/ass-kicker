package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.channel.impl.*;
import com.github.waitlight.asskicker.model.ChannelEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChannelFactory {

    private final WebClient webClient;
    private final ObjectMapper channelObjectMapper;

    public ChannelFactory(WebClient webClient,
            @Qualifier(ChannelObjectMapperConfig.BEAN_NAME) ObjectMapper channelObjectMapper) {
        this.webClient = webClient;
        this.channelObjectMapper = channelObjectMapper;
    }

    public Channel create(ChannelEntity provider) {
        if (provider == null) {
            throw new IllegalArgumentException("ChannelEntity must not be null");
        }

        try {
            return switch (provider.getProviderType()) {
                case APNS -> new ApnsPushChannel(provider, webClient, channelObjectMapper);
                case FCM -> new FcmPushChannel(provider, webClient, channelObjectMapper);
                case DINGTALK_BOT -> new DingtalkBotChannel(provider, webClient, channelObjectMapper);
                case ALIYUN_SMS -> new AliyunSmsChannel(provider, webClient, channelObjectMapper);
                case SMTP -> new SmtpEmailChannel(provider, webClient, channelObjectMapper);
                default -> {
                    log.warn("Unsupported channel provider type: {}", provider.getProviderType());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Failed to create Channel for channel {}: {}", provider.getCode(), e.getMessage(), e);
            return null;
        }
    }
}
