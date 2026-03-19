package com.github.waitlight.asskicker.channels.email;

import com.github.waitlight.asskicker.channels.Channel;

public abstract class EmailChannel<C extends EmailChannelProperty> extends Channel<C> {
    public EmailChannel(C config) {
        super(config);
    }
}
