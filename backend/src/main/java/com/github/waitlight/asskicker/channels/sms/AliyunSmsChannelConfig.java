package com.github.waitlight.asskicker.channels.sms;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class AliyunSmsChannelConfig extends SmsChannelConfig {

    @NotBlank
    private String accessKeyId;

    @NotBlank
    private String accessKeySecret;

    @NotBlank
    private String signName;

    /**
     * 单变量模板编码，如变量名为 content 则 TemplateParam 传 {"content": "全文"}
     */
    @NotBlank
    private String templateCode;

    /**
     * 模板中唯一变量的名称，用于直接发送完整内容，默认 content
     */
    private String templateParamKey = "content";

    private String regionId = "cn-hangzhou";

    @NotNull
    private Duration timeout = Duration.ofSeconds(10);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public AliyunSmsChannelConfig() {
        super("ALIYUN");
    }
}
