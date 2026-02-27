package com.github.waitlight.asskicker.channels.im;

import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IMChannelConfigConverterTest {

    private final IMChannelConfigConverter converter =
            new IMChannelConfigConverter(JsonMapper.builder().findAndAddModules().build(), validator());

    @Test
    void shouldConvertDingTalkFromRootAndExtractAccessToken() {
        Map<String, Object> properties = Map.of(
                "type", "DINGTALK",
                "webhookUrl", "https://oapi.dingtalk.com/robot/send?access_token=abc123",
                "secret", "s",
                "timeout", "PT6S",
                "maxRetries", 2,
                "retryDelay", "PT2S"
        );

        DingTalkIMChannelConfig config = assertInstanceOf(
                DingTalkIMChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("https://oapi.dingtalk.com/robot/send?access_token=abc123", config.getWebhookUrl());
        assertEquals("abc123", config.getAccessToken());
        assertEquals("s", config.getSecret());
        assertEquals(Duration.ofSeconds(6), config.getTimeout());
        assertEquals(2, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(2), config.getRetryDelay());
    }

    @Test
    void shouldConvertWechatWorkFromNestedConfig() {
        Map<String, Object> properties = Map.of(
                "protocol", "WECHAT_WORK",
                "wechatWork", Map.of(
                        "webhookUrl", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=123",
                        "timeout", "PT5S",
                        "maxRetries", 1,
                        "retryDelay", "PT1S"
                )
        );

        WechatWorkIMChannelConfig config = assertInstanceOf(
                WechatWorkIMChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=123", config.getWebhookUrl());
        assertEquals(Duration.ofSeconds(5), config.getTimeout());
        assertEquals(1, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(1), config.getRetryDelay());
    }

    @Test
    void shouldFailWhenDingTalkWebhookMissingAccessToken() {
        Map<String, Object> properties = Map.of(
                "type", "DINGTALK",
                "webhookUrl", "https://oapi.dingtalk.com/robot/send"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> converter.fromProperties(properties));

        assertEquals("钉钉 Webhook URL 缺少 access_token", ex.getReason());
    }

    @Test
    void shouldFailWhenMaxRetriesIsZero() {
        Map<String, Object> properties = Map.of(
                "type", "WECHAT_WORK",
                "webhookUrl", "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=123",
                "maxRetries", 0
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> converter.fromProperties(properties));

        assertEquals("WECHAT_WORK maxRetries 必须大于 0", ex.getReason());
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}