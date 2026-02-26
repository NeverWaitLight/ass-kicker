package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class IMChannelConfig implements ChannelConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public IMChannelConfig(String protocol) {
        this.protocol = protocol;
    }
}
