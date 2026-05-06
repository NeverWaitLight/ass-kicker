package com.github.waitlight.asskicker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProviderType {
    ALIYUN_SMS(ChannelType.SMS),
    AWS_SMS(ChannelType.SMS),

    SMTP(ChannelType.EMAIL),

    DINGTALK_WEBHOOK(ChannelType.IM),
    WECOM_WEBHOOK(ChannelType.IM),
    FEISHU_WEBHOOK(ChannelType.IM),
    DINGTALK_BOT(ChannelType.IM),
    WECOM_BOT(ChannelType.IM),
    FEISHU_BOT(ChannelType.IM),

    APNS(ChannelType.PUSH),
    FCM(ChannelType.PUSH),
    ;

    private final ChannelType channelType;
}
