package com.github.waitlight.asskicker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelProviderType {
    ALIYUN_SMS(ChannelType.SMS),
    TENCENT_SMS(ChannelType.SMS),
    AWS_SMS(ChannelType.SMS),

    ALIYUN_EMAIL(ChannelType.EMAIL),
    TENCENT_EMAIL(ChannelType.EMAIL),
    SMTP(ChannelType.EMAIL),
    AWS_EMAIL(ChannelType.EMAIL),

    DINGTALK(ChannelType.IM),
    WECOM(ChannelType.IM),
    FEISHU(ChannelType.IM),
    DINGTALK_BOT(ChannelType.IM),
    WECOM_BOT(ChannelType.IM),
    FEISHU_BOT(ChannelType.IM),
    SLACK(ChannelType.IM),

    APNS(ChannelType.PUSH),
    FCM(ChannelType.PUSH),
    ;

    private final ChannelType channelType;
}
