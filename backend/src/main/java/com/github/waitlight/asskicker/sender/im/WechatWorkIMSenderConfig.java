package com.github.waitlight.asskicker.sender.im;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class WechatWorkIMSenderConfig extends IMSenderConfig {

    @NotBlank
    private String webhookUrl;

    @NotNull
    private Duration timeout = Duration.ofSeconds(5);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public WechatWorkIMSenderConfig() {
        super("WECHAT_WORK");
    }
}
