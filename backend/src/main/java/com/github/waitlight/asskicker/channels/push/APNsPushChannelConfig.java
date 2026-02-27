package com.github.waitlight.asskicker.channels.push;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class APNsPushChannelConfig extends PushChannelConfig {

    @NotBlank
    private String teamId;

    @NotBlank
    private String keyId;

    @NotBlank
    private String bundleId;

    /**
     * p8 私钥内容（推荐）或通过 p8KeyPath 指定文件路径
     */
    private String p8KeyContent;

    private String p8KeyPath;

    private boolean production = true;

    @NotNull
    private Duration timeout = Duration.ofSeconds(10);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public APNsPushChannelConfig() {
        super("APNS");
    }
}
