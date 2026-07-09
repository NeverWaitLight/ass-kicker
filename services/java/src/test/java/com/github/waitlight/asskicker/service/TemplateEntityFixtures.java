package com.github.waitlight.asskicker.service;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.TemplateEntity;

/**
 * {@link TemplateEntity} 测试用固定样例，风格与
 * {@link ChannelEntityFixtures} 一致
 */
public final class TemplateEntityFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TemplateEntityFixtures() {
    }

    /**
     * 供 {@link com.github.waitlight.asskicker.service.TemplateServiceTest}
     */
    public static TemplateEntity smsCaptcha() {
        return read("""
                {
                  "code": "captcha",
                  "name": "短信验证码",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity emailCaptcha() {
        return read("""
                {
                  "code": "email-captcha",
                  "name": "邮件验证码",
                  "channelType": "EMAIL"
                }
                """);
    }

    public static TemplateEntity imOpsAlert() {
        return read("""
                {
                  "code": "ops_alert",
                  "name": "运维告警",
                  "channelType": "IM"
                }
                """);
    }

    public static TemplateEntity pushNewMessage() {
        return read("""
                {
                  "code": "new_message_push",
                  "name": "新消息推送",
                  "channelType": "PUSH"
                }
                """);
    }

    public static TemplateEntity welcomeEmail() {
        return read("""
                {
                  "code": "welcome",
                  "name": "欢迎邮件",
                  "channelType": "EMAIL"
                }
                """);
    }

    public static TemplateEntity smsCaptchaZhCn() {
        return read("""
                {
                  "id": "tpl-001",
                  "code": "sms_captcha",
                  "name": "短信验证码",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity localizedEmpty() {
        return read("""
                {
                  "id": "tpl-x",
                  "code": "x",
                  "name": "空本地化",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity localizedTemplatesNull() {
        return read("""
                {
                  "id": "tpl-x2",
                  "code": "x",
                  "name": "空本地化",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity greetEn() {
        return read("""
                {
                  "id": "tpl-greet",
                  "code": "greet",
                  "name": "问候",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity emptyBodyDe() {
        return read("""
                {
                  "id": "tpl-empty",
                  "code": "empty_body",
                  "name": "空正文",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity invZhCn() {
        return read("""
                {
                  "id": "tpl-inv",
                  "code": "inv",
                  "name": "库存",
                  "channelType": "SMS"
                }
                """);
    }

    public static TemplateEntity brandWelcomeEmail() {
        return read("""
                {
                  "id": "tpl-brand",
                  "code": "brand_welcome",
                  "name": "品牌欢迎邮件",
                  "channelType": "EMAIL"
                }
                """);
    }

    private static TemplateEntity read(String json) {
        try {
            return MAPPER.readValue(json, TemplateEntity.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}