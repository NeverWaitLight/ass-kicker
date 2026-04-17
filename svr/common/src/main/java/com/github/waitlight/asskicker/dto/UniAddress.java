package com.github.waitlight.asskicker.dto;

import java.util.Set;

import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.model.ChannelType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
    private ProviderType providerType;
    private String channelKey;
    private Set<String> recipients;

    public static UniAddress ofSms(String... phoneNumber) {
        return UniAddress.builder()
                .channelType(ChannelType.SMS)
                .recipients(Set.of(phoneNumber))
                .build();
    }

    public static UniAddress ofEmail(String... emailAddress) {
        return UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .recipients(Set.of(emailAddress))
                .build();
    }

    public static UniAddress ofImBot(ProviderType provider, String channelKey, String... targetId) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .providerType(provider)
                .channelKey(channelKey)
                .recipients(Set.of(targetId))
                .build();
    }

    public static UniAddress ofImWebhook(ProviderType provider, String... webhookToken) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .providerType(provider)
                .recipients(Set.of(webhookToken))
                .build();
    }

    public static UniAddress ofPush(ProviderType provider, String... deviceToken) {
        return UniAddress.builder()
                .channelType(ChannelType.PUSH)
                .providerType(provider)
                .recipients(Set.of(deviceToken))
                .build();
    }
}
