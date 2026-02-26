package com.github.waitlight.asskicker.sender.im;

import com.github.waitlight.asskicker.sender.SenderConfig;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class IMSenderConfig implements SenderConfig {
    @Getter(AccessLevel.PROTECTED)
    protected final String protocol;

    public IMSenderConfig(String protocol) {
        this.protocol = protocol;
    }
}
