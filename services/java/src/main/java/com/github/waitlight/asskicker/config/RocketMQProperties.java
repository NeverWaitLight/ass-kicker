package com.github.waitlight.asskicker.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ass-kicker.rocketmq")
public class RocketMQProperties {

    @NotBlank
    private String sendReqsTopic;
}
