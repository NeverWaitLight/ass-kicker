package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.channel.debug")
public class ChannelDebugProperties {

    private boolean enabled = false;

    @Min(0)
    private int minSleepMs = 60;

    @Min(0)
    private int maxSleepMs = 120;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinSleepMs() {
        return minSleepMs;
    }

    public void setMinSleepMs(int minSleepMs) {
        this.minSleepMs = minSleepMs;
    }

    public int getMaxSleepMs() {
        return maxSleepMs;
    }

    public void setMaxSleepMs(int maxSleepMs) {
        this.maxSleepMs = maxSleepMs;
    }

    @AssertTrue(message = "app.channel.debug.max-sleep-ms must be greater than or equal to min-sleep-ms")
    public boolean isSleepRangeValid() {
        return maxSleepMs >= minSleepMs;
    }
}
