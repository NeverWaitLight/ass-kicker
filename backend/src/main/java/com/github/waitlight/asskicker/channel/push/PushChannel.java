package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.Channel;

public abstract class PushChannel<C extends PushChannelProperty> extends Channel<C> {

    public PushChannel(C config) {
        super(config);
    }
}
