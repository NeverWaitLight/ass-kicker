package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class SmsChannelConfig implements ChannelConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public SmsChannelConfig(String protocol) {
        this.protocol = protocol;
    }
}
