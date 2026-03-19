package com.github.waitlight.asskicker.channel.sms;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;

public abstract class SmsChannel<C extends SmsChannelProperties> extends Channel<C> {

    public SmsChannel(C config, ChannelDebugProperties debugProperties) {
        super(config, debugProperties);
    }
}
