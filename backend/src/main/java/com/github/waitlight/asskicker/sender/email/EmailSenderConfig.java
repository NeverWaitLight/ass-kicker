package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.SenderConfig;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class EmailSenderConfig implements SenderConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final EmailProtocol protocol;

    public EmailSenderConfig(EmailProtocol protocol) {
        this.protocol = protocol;
    }
}
