package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;

public abstract class IMChannel<C extends IMChannelProperties> extends Channel<C> {

    public IMChannel(C config, ChannelDebugProperties debugProperties) {
        super(config, debugProperties);
    }
}
