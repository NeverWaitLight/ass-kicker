package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ImReq extends SendReq {
    /** IM 机器人 webhook token */
    @NotBlank
    private String token;

    /** 机器人加签模式下的密钥,用于 HMAC-SHA256 签名;若机器人未启用加签可留空 */
    private String secret;

    /** 消息文本内容 */
    @NotBlank
    private String content;

    @Override
    public void applyRendered(String title, String content) {
        if (content != null && !content.isBlank()) this.content = content;
    }
}
