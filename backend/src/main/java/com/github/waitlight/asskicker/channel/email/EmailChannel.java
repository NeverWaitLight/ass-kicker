package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;

public abstract class EmailChannel<C extends EmailChannelProperties> extends Channel<C> {

    public EmailChannel(C config, ChannelDebugProperties debugProperties) {
        super(config, debugProperties);
    }
}
