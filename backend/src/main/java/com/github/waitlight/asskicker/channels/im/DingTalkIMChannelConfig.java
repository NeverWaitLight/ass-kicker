package com.github.waitlight.asskicker.channels.im;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class DingTalkIMChannelConfig extends IMChannelConfig {

    @NotBlank
    private String webhookUrl;

    // access_token 从 webhookUrl 中提取，不需要用户单独配置
    private String accessToken;

    private String secret;

    @NotNull
    private Duration timeout = Duration.ofSeconds(5);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public DingTalkIMChannelConfig() {
        super("DINGTALK");
    }
}
