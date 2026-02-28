package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ass-kicker.channel.debug")
public class ChannelDebugProperties {

    private boolean enabled = false;

    @Min(0)
    private int sleepMs = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSleepMs() {
        return sleepMs;
    }

    public void setSleepMs(int sleepMs) {
        this.sleepMs = sleepMs;
    }
}
