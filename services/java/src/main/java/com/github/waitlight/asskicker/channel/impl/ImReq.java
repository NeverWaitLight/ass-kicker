package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.ChannelReq;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImReq extends ChannelReq {
    /**
     * IM 机器人 webhook token:
     * - DingTalk: webhook URL 中的 access_token 参数值
     * - Feishu: webhook URL 中最后一段 hook token(即 /open-apis/bot/v2/hook/{token})
     */
    @NotBlank
    private String token;

    /** 机器人加签模式下的密钥,用于 HMAC-SHA256 签名;若机器人未启用加签可留空 */
    private String secret;

    /** 消息文本内容 */
    @NotBlank
    private String content;
}
