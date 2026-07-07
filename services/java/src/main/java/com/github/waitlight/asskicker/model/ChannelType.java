package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelType {
    SMS(ChannelGroup.SMS),
    EMAIL(ChannelGroup.EMAIL),
    DINGTALK(ChannelGroup.IM),
    FEISHU(ChannelGroup.IM),
    APNS(ChannelGroup.PUSH),
    FCM(ChannelGroup.PUSH);

    private final ChannelGroup group;
}
