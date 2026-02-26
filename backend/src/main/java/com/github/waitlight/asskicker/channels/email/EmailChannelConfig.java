package com.github.waitlight.asskicker.channels.email;

import com.github.waitlight.asskicker.channels.ChannelConfig;

import lombok.AccessLevel;
import lombok.Getter;

public abstract class EmailChannelConfig implements ChannelConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final EmailChannelType protocol;

    public EmailChannelConfig(EmailChannelType protocol) {
        this.protocol = protocol;
    }
}
