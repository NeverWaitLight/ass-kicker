package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.ChannelProperty;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class IMChannelProperty implements ChannelProperty {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public IMChannelProperty(String protocol) {
        this.protocol = protocol;
    }
}
