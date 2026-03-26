package com.github.waitlight.asskicker.channelhandler;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

/**
 * {@link ApnsChannelHandlerTest} 使用的渠道配置样例
 */
public final class ApnsChannelHandlerFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApnsChannelHandlerFixtures() {
    }

    /**
     * 合法 APNS 渠道配置（p8 占位即可）供 handler 走完整校验与
     * {@link com.github.waitlight.asskicker.channel.Channel#send}；
     * 单测中需配合
     * {@link com.github.waitlight.asskicker.config.ChannelDebugProperties#setEnabled(boolean)}
     * 避免真实请求
     */
    public static ChannelProviderEntity pushApnsValid() {
        return read("""
                {
                  "name": "APNs 测试",
                  "key": "apns-test",
                  "type": "PUSH",
                  "provider": "APNS",
                  "description": "APNs valid fixture for handler tests",
                  "enabled": true,
                  "properties": {
                    "type": "APNS",
                    "teamId": "TEAM1",
                    "keyId": "KEY1",
                    "bundleId": "com.example.app",
                    "p8KeyContent": "placeholder-not-used-when-debug-enabled",
                    "production": false,
                    "timeout": 10000,
                    "retryDelay": 1000,
                    "maxRetries": 0
                  }
                }
                """);
    }

    /**
     * 与 APNs handler 地址混用时的对照样例：PUSH 但 provider 为 FCM，用于断言“不是 APNS”分支
     */
    public static ChannelProviderEntity pushFcmProd() {
        return read("""
                {
                  "name": "FCM 推送",
                  "key": "fcm-prod",
                  "type": "PUSH",
                  "provider": "FCM",
                  "description": "FCM for handler tests",
                  "enabled": true,
                  "properties": {}
                }
                """);
    }

    private static ChannelProviderEntity read(String json) {
        try {
            return MAPPER.readValue(json, ChannelProviderEntity.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
