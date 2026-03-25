package com.github.waitlight.asskicker;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.service.ChannelProviderEntityFixtures;

/**
 * {@link MessageTemplateEntity} 测试用固定样例，风格与
 * {@link ChannelProviderEntityFixtures} 一致；仅使用 {@code localizedTemplates} 等当前字段
 */
public final class MessageTemplateEntityFixtures {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MessageTemplateEntityFixtures() {
  }

  /**
   * 供 {@link com.github.waitlight.asskicker.service.MessageTemplateServiceTest}
   */
  public static MessageTemplateEntity smsCaptcha() {
    return read("""
        {
          "code": "captcha",
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "content": "{$channel.signName}这是登录验证码 {$code}" },
            "en": { "content": "{$channel.signName}This is your captcha {$code}" }
          }
        }
        """);
  }

  public static MessageTemplateEntity emailCaptcha() {
    return read("""
        {
          "code": "email-captcha",
          "channelType": "EMAIL",
          "localizedTemplates": {
            "zh-CN": { "title": "验证码", "content": "这是你的验证码 {$code}" },
            "en": { "title": "Verification code", "content": "This is your verification code {$code}" }
          }
        }
        """);
  }

  public static MessageTemplateEntity imOpsAlert() {
    return read("""
        {
          "code": "ops_alert",
          "channelType": "IM",
          "localizedTemplates": {
            "zh-CN": { "title": "告警", "content": "服务 {$service} 出现异常" },
            "en": { "title": "Alert", "content": "Service {$service} has an issue" }
          }
        }
        """);
  }

  public static MessageTemplateEntity pushNewMessage() {
    return read("""
        {
          "code": "new_message_push",
          "channelType": "PUSH",
          "localizedTemplates": {
            "zh-CN": { "title": "新消息", "content": "对方昵称:消息摘要..." },
            "en": { "title": "New Message", "content": "nickname:shour message..." }
          }
        }
        """);
  }

  public static MessageTemplateEntity welcomeEmail() {
    return read("""
        {
          "code": "welcome",
          "channelType": "EMAIL",
          "localizedTemplates": {
            "zh-CN": { "title": "欢迎", "content": "欢迎 {$name}" },
            "en": { "title": "Welcome", "content": "Welcome {$name}" }
          }
        }
        """);
  }

  public static MessageTemplateEntity smsCaptchaZhCn() {
    return read("""
        {
          "code": "sms_captcha",
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "title": "验证码", "content": "您好 {{name}}，您的验证码是 {{code}}" }
          }
        }
        """);
  }

  public static MessageTemplateEntity localizedEmpty() {
    return read("""
        {
          "code": "x",
          "channelType": "SMS",
          "localizedTemplates": {}
        }
        """);
  }

  public static MessageTemplateEntity localizedTemplatesNull() {
    return read("""
        {
          "code": "x",
          "channelType": "SMS",
          "localizedTemplates": null
        }
        """);
  }

  public static MessageTemplateEntity greetEn() {
    return read("""
        {
          "code": "greet",
          "channelType": "SMS",
          "localizedTemplates": {
            "en": { "title": "t", "content": "Hello {{name}}" }
          }
        }
        """);
  }

  public static MessageTemplateEntity emptyBodyDe() {
    return read("""
        {
          "code": "empty_body",
          "channelType": "SMS",
          "localizedTemplates": {
            "de": { "title": "only title", "content": null }
          }
        }
        """);
  }

  public static MessageTemplateEntity invZhCn() {
    return read("""
        {
          "code": "inv",
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "title": "t", "content": "x {{p}}" }
          }
        }
        """);
  }

  private static MessageTemplateEntity read(String json) {
    try {
      return MAPPER.readValue(json, MessageTemplateEntity.class);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
