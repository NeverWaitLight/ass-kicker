package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.Channel;

public abstract class SmsChannel<C extends SmsChannelProperty> extends Channel<C> {

    public SmsChannel(C config) {
        super(config);
    }
}
