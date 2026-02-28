package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ass-kicker.cache")
public class CaffeineCacheProperties {

    @Min(1)
    private long maximumSize = 1000;

    @Min(1)
    private long expireAfterWriteMinutes = 10;

    @Min(0)
    @Max(100)
    private int randomJitterPercent = 20;
}
