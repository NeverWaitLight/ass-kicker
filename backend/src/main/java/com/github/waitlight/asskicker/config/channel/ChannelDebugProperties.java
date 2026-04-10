package com.github.waitlight.asskicker.config.channel;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ass-kicker.channel.debug")
public class ChannelDebugProperties {

    private boolean enabled = false;

    @Min(0)
    private int sleepMs = 100;
}