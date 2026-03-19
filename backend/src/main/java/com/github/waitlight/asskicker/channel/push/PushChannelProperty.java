package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.ChannelProperty;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class PushChannelProperty implements ChannelProperty {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public PushChannelProperty(String protocol) {
        this.protocol = protocol;
    }
}
