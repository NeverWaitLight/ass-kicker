package com.github.waitlight.asskicker.channel.sms;

import com.github.waitlight.asskicker.channel.ChannelProperty;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class SmsChannelProperty implements ChannelProperty {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public SmsChannelProperty(String protocol) {
        this.protocol = protocol;
    }
}
