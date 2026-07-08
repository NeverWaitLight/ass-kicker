package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.ChannelReq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class PushReq extends ChannelReq {
    /** 目标设备令牌:APNs device token 或 FCM registration token */
    @NotBlank
    private String deviceToken;

    /** 通知标题 */
    @NotBlank
    private String title;

    /** 通知正文 */
    @NotBlank
    private String body;

    /** 投递优先级:APNs 取 IMMEDIATE/CONSERVE_POWER,FCM 取 HIGH/NORMAL,空则用平台默认 */
    @Pattern(regexp = "^(IMMEDIATE|CONSERVE_POWER|HIGH|NORMAL)$")
    private String priority;

    /** 自定义数据字段,APNs 作为 payload 自定义键,FCM 转为 data map(值会 toString) */
    private Map<String, Object> data;
}
