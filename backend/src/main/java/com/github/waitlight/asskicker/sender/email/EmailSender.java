package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.Sender;

public abstract class EmailSender<C extends EmailSenderConfig> extends Sender<C> {
    public EmailSender(C config) {
        super(config);
    }
}
