package com.github.waitlight.asskicker.dto.send;

import com.github.waitlight.asskicker.model.ChannelType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一联系地址模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UniAddress {

    // 1. 通道大类 (SMS, EMAIL, IM, PUSH)
    private ChannelType channelType;

    // 2. 服务提供商 (TELEGRAM, DINGTALK, WECOM, LARK, APNS, FCM)
    private String channelProvider;

    // 3. 发送方标识/配置Key (例如：配置中心里的 telegram-bot-01，或者企微的 agentId)
    // 注意：对于群 Webhook 机器人，此项通常为空，因为 Webhook 自身包含了身份。
    private String channelKey;

    // 4. 接收方目标地址 (手机号、邮箱、chat_id、userId、设备Token、Webhook URL后缀等)
    private String recipient;

    /**
     * 传统通道：短信
     * 例：SMS - null - null - 13800138000
     */
    public static UniAddress ofSms(String phoneNumber) {
        return UniAddress.builder()
                .channelType(ChannelType.SMS)
                .recipient(phoneNumber)
                .build();
    }

    /**
     * 传统通道：邮件
     * 例：EMAIL - null - null - test@example.com
     */
    public static UniAddress ofEmail(String emailAddress) {
        return UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .recipient(emailAddress)
                .build();
    }

    /**
     * 【场景A：全局应用 Bot 模式】 (支持：Telegram, 钉钉企业内部机器人, 企微自建应用, 飞书应用)
     * 特点：需要指定特定的 Bot (senderKey) 发送给特定的 用户/群 (target)
     * 例：IM_BOT - TELEGRAM - my-tg-bot-1 - 123456789 (chat_id)
     * 例：IM_BOT - LARK - my-feishu-app - ou_12345abcde (open_id)
     */
    public static UniAddress ofImBot(String provider, String channelKey, String targetId) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .channelProvider(provider)
                .channelKey(channelKey)
                .recipient(targetId)
                .build();
    }

    /**
     * 【场景B：群自定义 Webhook 机器人模式】 (支持：钉钉群, 企微群, 飞书群。注：Telegram不支持此模式)
     * 特点：不需要指定 senderKey，因为 Webhook Token 本身既代表了发送方，也代表了接收方(特定群)。
     * 例：IM_WEBHOOK - DINGTALK - null - ebf889xyz... (webhook token)
     */
    public static UniAddress ofImWebhook(String provider, String webhookToken) {
        return UniAddress.builder()
                .channelType(ChannelType.IM)
                .channelProvider(provider)
                .recipient(webhookToken)
                .build();
    }

    /**
     * 推送通道：APNS / FCM
     * 例：PUSH - APNS - null - device_token_abc123
     */
    public static UniAddress ofPush(String provider, String deviceToken) {
        return UniAddress.builder()
                .channelType(ChannelType.PUSH)
                .channelProvider(provider)
                .recipient(deviceToken)
                .build();
    }
}
