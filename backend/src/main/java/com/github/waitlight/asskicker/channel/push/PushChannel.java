package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;

public abstract class PushChannel<C extends PushChannelProperties> extends Channel<C> {

    public PushChannel(C config, ChannelDebugProperties debugProperties) {
        super(config, debugProperties);
    }
}
