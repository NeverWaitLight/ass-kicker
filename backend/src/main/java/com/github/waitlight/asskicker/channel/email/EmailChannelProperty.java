package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.ChannelProperty;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class EmailChannelProperty implements ChannelProperty {
    @Getter(AccessLevel.PROTECTED)
    protected final EmailChannelType protocol;

    public EmailChannelProperty(EmailChannelType protocol) {
        this.protocol = protocol;
    }
}
