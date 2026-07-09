package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChannelDebugConfig.ChannelDebugProperties.class)
public class ChannelDebugConfig {

    @Getter
    @Setter
    @Validated
    @ConfigurationProperties(prefix = "ass-kicker.channel.debug")
    public static class ChannelDebugProperties {

        private boolean enabled = false;

        @Min(0)
        private int sleepMs = 100;
    }
}
