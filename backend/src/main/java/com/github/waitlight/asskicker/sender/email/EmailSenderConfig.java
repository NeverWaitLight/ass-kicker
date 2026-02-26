package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.SenderConfig;

import lombok.AccessLevel;
import lombok.Getter;

public abstract class EmailSenderConfig implements SenderConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final EmailSenderType protocol;

    public EmailSenderConfig(EmailSenderType protocol) {
        this.protocol = protocol;
    }
}
