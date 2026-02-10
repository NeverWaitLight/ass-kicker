package com.github.waitlight.asskicker.testsend;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.test-send")
public class TestSendProperties {

    @Min(1)
    private int maxRequests = 5;

    @NotNull
    private Duration window = Duration.ofMinutes(1);

    @Min(1)
    private int maxTargetLength = 255;

    @Min(1)
    private int maxContentLength = 2000;

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public int getMaxTargetLength() {
        return maxTargetLength;
    }

    public void setMaxTargetLength(int maxTargetLength) {
        this.maxTargetLength = maxTargetLength;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }
}
