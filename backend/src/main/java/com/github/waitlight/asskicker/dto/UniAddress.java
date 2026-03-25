package com.github.waitlight.asskicker.dto;

import com.github.waitlight.asskicker.model.ChannelProviderType;
import com.github.waitlight.asskicker.model.ChannelType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UniAddress {

    private ChannelType channelType;
    private ChannelProviderType channelProviderType;
    private String channelProviderKey;
    private String recipient;

    public static UniAddress ofSms(String phoneNumber) {
        return UniAddress.builder()
                .channelType(ChannelType.SMS)
                .recipient(phoneNumber)
                .build();
    }

    public static UniAddress ofEmail(String emailAddress) {
        return UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .recipient(emailAddress)
                .build();
    }

    public static UniAddress ofImBot(ChannelProviderType provider, String channelKey, String targetId) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .channelProviderType(provider)
                .channelProviderKey(channelKey)
                .recipient(targetId)
                .build();
    }

    public static UniAddress ofImWebhook(ChannelProviderType provider, String webhookToken) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .channelProviderType(provider)
                .recipient(webhookToken)
                .build();
    }

    public static UniAddress ofPush(ChannelProviderType provider, String deviceToken) {
        return UniAddress.builder()
                .channelType(ChannelType.PUSH)
                .channelProviderType(provider)
                .recipient(deviceToken)
                .build();
    }
}
