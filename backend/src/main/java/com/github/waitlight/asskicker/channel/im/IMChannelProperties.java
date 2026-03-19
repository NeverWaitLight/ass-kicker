package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.ChannelProperties;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class IMChannelProperties implements ChannelProperties {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public IMChannelProperties(String protocol) {
        this.protocol = protocol;
    }
}
