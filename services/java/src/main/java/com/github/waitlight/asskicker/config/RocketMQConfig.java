package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RocketMQConfig.RocketMQProperties.class)
public class RocketMQConfig {

    @Getter
    @Setter
    @Validated
    @ConfigurationProperties(prefix = "ass-kicker.rocketmq")
    public static class RocketMQProperties {

        @NotBlank
        private String sendReqsTopic;
    }
}
