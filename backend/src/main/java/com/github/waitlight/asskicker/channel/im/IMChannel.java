package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.Channel;

public abstract class IMChannel<C extends IMChannelProperty> extends Channel<C> {

    public IMChannel(C config) {
        super(config);
    }

}
