package com.github.waitlight.asskicker.channels.push;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class PushChannelConfig implements ChannelConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public PushChannelConfig(String protocol) {
        this.protocol = protocol;
    }
}
