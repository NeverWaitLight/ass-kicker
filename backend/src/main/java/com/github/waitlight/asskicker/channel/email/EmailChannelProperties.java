package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.ChannelProperties;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class EmailChannelProperties implements ChannelProperties {
    @Getter(AccessLevel.PROTECTED)
    protected final EmailChannelType protocol;

    public EmailChannelProperties(EmailChannelType protocol) {
        this.protocol = protocol;
    }
}
