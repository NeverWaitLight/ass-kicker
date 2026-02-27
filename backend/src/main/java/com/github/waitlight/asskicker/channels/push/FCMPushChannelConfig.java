package com.github.waitlight.asskicker.channels.push;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class FCMPushChannelConfig extends PushChannelConfig {

    /**
     * 服务账号 JSON 文件路径或 JSON 字符串内容
     */
    @NotBlank
    private String serviceAccountJson;

    /**
     * 可选，若未设置则从 serviceAccountJson 中解析 project_id
     */
    private String projectId;

    @NotNull
    private Duration timeout = Duration.ofSeconds(10);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);

    public FCMPushChannelConfig() {
        super("FCM");
    }
}
