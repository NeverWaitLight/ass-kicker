package com.github.waitlight.asskicker.channels.push;

import com.github.waitlight.asskicker.channels.Channel;

public abstract class PushChannel<C extends PushChannelProperty> extends Channel<C> {

    public PushChannel(C config) {
        super(config);
    }
}
