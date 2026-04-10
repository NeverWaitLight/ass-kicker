package com.github.waitlight.asskicker.channel;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.config.channel.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

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

    public Channel create(ChannelProviderEntity provider) {
        Assert.notNull(provider, "ChannelProviderEntity must not be null");

        try {
            return switch (provider.getProviderType()) {
                case APNS -> new ApnsChannel(provider, webClient, channelObjectMapper);
                case FCM -> new FcmChannel(provider, webClient, channelObjectMapper);
                case DINGTALK_WEBHOOK -> new DingtalkWebhookChannel(provider, webClient, channelObjectMapper);
                case WECOM_WEBHOOK -> new WecomWebhookChannel(provider, webClient, channelObjectMapper);
                case FEISHU_WEBHOOK -> new FeishuWebhookChannel(provider, webClient, channelObjectMapper);
                case DINGTALK_BOT -> new DingtalkBotChannel(provider, webClient, channelObjectMapper);
                case WECOM_BOT -> new WecomBotChannel(provider, webClient, channelObjectMapper);
                case FEISHU_BOT -> new FeishuBotChannel(provider, webClient, channelObjectMapper);
                case ALIYUN_SMS -> new AliyunSmsChannel(provider, webClient, channelObjectMapper);
                case AWS_SMS -> new AwsSnsSmsChannel(provider, webClient, channelObjectMapper);
                case SMTP -> new SmtpChannel(provider, webClient, channelObjectMapper);
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
