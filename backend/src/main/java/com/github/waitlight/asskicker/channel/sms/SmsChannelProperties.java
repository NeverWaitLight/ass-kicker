package com.github.waitlight.asskicker.channel.sms;

import com.github.waitlight.asskicker.channel.ChannelProperties;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class SmsChannelProperties implements ChannelProperties {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public SmsChannelProperties(String protocol) {
        this.protocol = protocol;
    }
}
