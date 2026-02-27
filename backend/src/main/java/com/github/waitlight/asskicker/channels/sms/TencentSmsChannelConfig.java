package com.github.waitlight.asskicker.channels.sms;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class TencentSmsChannelConfig extends SmsChannelConfig {

    @NotBlank
    private String secretId;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String sdkAppId;

    @NotBlank
    private String signName;

    /**
     * 单变量模板 ID，模板参数传单元素数组 [全文内容]
     */
    @NotBlank
    private String templateId;

    private String region = "ap-guangzhou";

    @NotNull
    private Duration timeout = Duration.ofSeconds(10);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public TencentSmsChannelConfig() {
        super("TENCENT");
    }
}
