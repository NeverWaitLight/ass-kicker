package com.github.waitlight.asskicker.channel.sms;

import com.github.waitlight.asskicker.channel.Channel;

public abstract class SmsChannel<C extends SmsChannelProperty> extends Channel<C> {

    public SmsChannel(C config) {
        super(config);
    }
}
