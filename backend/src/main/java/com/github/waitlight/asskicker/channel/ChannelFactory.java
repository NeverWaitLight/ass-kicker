package com.github.waitlight.asskicker.channel;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelFactory {

    @Qualifier("sharedWebClient")
    private final WebClient webClient;

    public Channel create(ChannelProviderEntity provider) {
        Assert.notNull(provider, "ChannelProviderEntity must not be null");

        try {
            return switch (provider.getProviderType()) {
                case APNS -> new ApnsChannel(provider, webClient);
                case FCM -> new FcmChannel(provider, webClient);
                case DINGTALK -> new DingtalkWebhookChannel(provider, webClient);
                case WECOM -> new WecomWebhookChannel(provider, webClient);
                case FEISHU -> new FeishuWebhookChannel(provider, webClient);
                case DINGTALK_BOT -> new DingtalkBotChannel(provider, webClient);
                case WECOM_BOT -> new WecomBotChannel(provider, webClient);
                case FEISHU_BOT -> new FeishuBotChannel(provider, webClient);
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

    public List<ChannelProviderType> getSupportedTypes() {
        return List.of(
                ChannelProviderType.APNS,
                ChannelProviderType.FCM,
                ChannelProviderType.DINGTALK,
                ChannelProviderType.WECOM,
                ChannelProviderType.FEISHU,
                ChannelProviderType.DINGTALK_BOT,
                ChannelProviderType.WECOM_BOT,
                ChannelProviderType.FEISHU_BOT);
    }
}
