package com.github.waitlight.asskicker.sender.im;

import com.github.waitlight.asskicker.sender.Sender;

public abstract class IMSender<C extends IMSenderConfig> extends Sender<C> {

    public IMSender(C config) {
        super(config);
    }

}
