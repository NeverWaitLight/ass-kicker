package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.ChannelProperties;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class PushChannelProperties implements ChannelProperties {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public PushChannelProperties(String protocol) {
        this.protocol = protocol;
    }
}
