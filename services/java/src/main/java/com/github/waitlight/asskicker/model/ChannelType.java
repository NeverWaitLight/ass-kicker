package com.github.waitlight.asskicker.model;

/**
 * 消息渠道类型
 * 按短信、邮件、即时通讯、推送等维度划分，用于对服务商（ProviderType）进行分类与路由
 */
public enum ChannelType {
    /** 短信（Short Message Service） */
    SMS,
    /** 邮件（Electronic Mail） */
    EMAIL,
    /** 即时通讯（Instant Messaging） */
    IM,
    /** 移动端推送（Push Notification） */
    PUSH;
}
