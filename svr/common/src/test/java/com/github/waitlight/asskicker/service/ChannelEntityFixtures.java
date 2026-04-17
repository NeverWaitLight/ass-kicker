package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelEntity;

import java.io.UncheckedIOException;

/**
 * 文档 {@code docs/message-template-and-channel-provider.md} 中四类渠道的代表性样例，
 * 用于 ChannelService 内存 Mongo 测试。
 */
public final class ChannelEntityFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChannelEntityFixtures() {
    }

    public static ChannelEntity smsAliyun() {
        return read("""
                {
                  "name": "阿里云短信",
                  "code": "aliyun-sms-prod",
                  "channelType": "SMS",
                  "providerType": "ALIYUN_SMS",
                  "description": "Aliyun SMS",
                  "priorityAddressRegex": "^1[3-9]\\\\d{9}$",
                  "excludeAddressRegex": "^1(70|71|162)\\\\d{8}$",
                  "enabled": true,
                  "properties": {
                    "accessKeyId": "ak-test",
                    "accessKeySecret": "sk-test",
                    "signName": "签名",
                    "regionId": "cn-hangzhou",
                    "maxRetries": "3",
                    "timeout": "10000",
                    "retryDelay": "1000"
                  }
                }
                """);
    }

    public static ChannelEntity emailSmtp() {
        return read("""
                {
                  "name": "SMTP 邮件",
                  "code": "smtp-email-prod",
                  "channelType": "EMAIL",
                  "providerType": "SMTP",
                  "description": "SMTP channel",
                  "priorityAddressRegex": "^[a-zA-Z0-9._%+-]+@(mail\\\\.)?corp\\\\.example\\\\.com$",
                  "excludeAddressRegex": "^(no-?reply|bounce|mailer-daemon)@",
                  "enabled": true,
                  "properties": {
                    "host": "smtp.example.com",
                    "port": "465",
                    "username": "user@example.com",
                    "password": "pass",
                    "sslEnabled": "true",
                    "from": "noreply@example.com",
                    "connectionTimeout": "5000",
                    "readTimeout": "10000",
                    "maxRetries": "3",
                    "retryDelay": "1000"
                  }
                }
                """);
    }

    public static ChannelEntity imSlackEnabled() {
        return read("""
                {
                  "name": "钉钉 Webhook 渠道",
                  "code": "dingtalk-webhook-prod",
                  "channelType": "IM",
                  "providerType": "DINGTALK_WEBHOOK",
                  "description": "DingTalk IM webhook",
                  "priorityAddressRegex": "^(C|G|D)[A-Z0-9]{8,}$|^#[a-z0-9._-]+$",
                  "excludeAddressRegex": "^D0[A-Z0-9]+$|^#archive-",
                  "enabled": true,
                  "properties": {
                    "url": "https://oapi.dingtalk.com/robot/send",
                    "maxRetries": "3",
                    "timeout": "10000",
                    "retryDelay": "1000"
                  }
                }
                """);
    }

    public static ChannelEntity imSlackDisabled() {
        return read("""
                {
                  "name": "钉钉 Webhook 停用",
                  "code": "dingtalk-webhook-disabled",
                  "channelType": "IM",
                  "providerType": "DINGTALK_WEBHOOK",
                  "description": "disabled",
                  "priorityAddressRegex": "^(C|G|D)[A-Z0-9]{8,}$",
                  "excludeAddressRegex": "^#archive-",
                  "enabled": false,
                  "properties": {
                    "url": ""
                  }
                }
                """);
    }

    public static ChannelEntity pushApnsEnabled() {
        return read("""
                {
                  "name": "苹果推送",
                  "code": "apns-app-dev",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "description": "APNs push",
                  "priorityAddressRegex": "^[0-9a-fA-F]{64}$",
                  "excludeAddressRegex": "^0{64}$|^[fF]{64}$",
                  "enabled": true,
                  "properties": {
                    "teamId": "APPLE_TEAM_ID",
                    "keyId": "AUTH_KEY_ID",
                    "bundleId": "com.example.app",
                    "p8KeyContent": "-----BEGIN PRIVATE KEY-----\\nTEST\\n-----END PRIVATE KEY-----\\n",
                    "production": "false",
                    "maxRetries": "3",
                    "timeout": "10000",
                    "retryDelay": "1000"
                  }
                }
                """);
    }

    public static ChannelEntity pushApnsDisabled() {
        return read("""
                {
                  "name": "苹果推送停用",
                  "code": "apns-app-disabled",
                  "channelType": "PUSH",
                  "providerType": "APNS",
                  "description": "disabled",
                  "priorityAddressRegex": "^[0-9a-fA-F]{64}$",
                  "excludeAddressRegex": "^0{64}$",
                  "enabled": false,
                  "properties": {
                    "teamId": "T",
                    "keyId": "K",
                    "bundleId": "com.example.off",
                    "p8KeyContent": "-----BEGIN PRIVATE KEY-----\\nOFF\\n-----END PRIVATE KEY-----\\n",
                    "production": "false"
                  }
                }
                """);
    }

    private static ChannelEntity read(String json) {
        try {
            return MAPPER.readValue(json, ChannelEntity.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}