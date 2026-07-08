package com.github.waitlight.asskicker.dto;

import java.util.Set;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @deprecated 已被 {@link com.github.waitlight.asskicker.channel.SendReq} 体系取代。
 */
@Deprecated
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UniAddress {

    private ChannelType channelType;
    private ChannelProvider provider;
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

}
