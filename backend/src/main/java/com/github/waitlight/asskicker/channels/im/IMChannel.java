package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.Channel;

public abstract class IMChannel<C extends IMChannelConfig> extends Channel<C> {

    public IMChannel(C config) {
        super(config);
    }

}
