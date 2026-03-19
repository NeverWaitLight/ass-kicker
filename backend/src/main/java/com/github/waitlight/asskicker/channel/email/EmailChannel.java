package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.Channel;

public abstract class EmailChannel<C extends EmailChannelProperty> extends Channel<C> {
    public EmailChannel(C config) {
        super(config);
    }
}
