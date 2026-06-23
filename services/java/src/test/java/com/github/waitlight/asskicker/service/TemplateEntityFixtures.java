package com.github.waitlight.asskicker.service;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.TemplateEntity;

/**
 * {@link TemplateEntity} 测试用固定样例，风格与
 * {@link ChannelEntityFixtures} 一致；仅使用 {@code localizedTemplates} 等当前字段
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
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "content": "{$channel.signName}这是登录验证码 {$code}" },
            "en": { "content": "{$channel.signName}This is your captcha {$code}" }
          }
        }
        """);
  }

  public static TemplateEntity emailCaptcha() {
    return read("""
        {
          "code": "email-captcha",
          "name": "邮件验证码",
          "channelType": "EMAIL",
          "localizedTemplates": {
            "zh-CN": { "title": "验证码", "content": "这是你的验证码 {$code}" },
            "en": { "title": "Verification code", "content": "This is your verification code {$code}" }
          }
        }
        """);
  }

  public static TemplateEntity imOpsAlert() {
    return read("""
        {
          "code": "ops_alert",
          "name": "运维告警",
          "channelType": "IM",
          "localizedTemplates": {
            "zh-CN": { "title": "告警", "content": "服务 {$service} 出现异常" },
            "en": { "title": "Alert", "content": "Service {$service} has an issue" }
          }
        }
        """);
  }

  public static TemplateEntity pushNewMessage() {
    return read("""
        {
          "code": "new_message_push",
          "name": "新消息推送",
          "channelType": "PUSH",
          "localizedTemplates": {
            "zh-CN": { "title": "新消息", "content": "对方昵称:消息摘要..." },
            "en": { "title": "New Message", "content": "nickname:shour message..." }
          }
        }
        """);
  }

  public static TemplateEntity welcomeEmail() {
    return read("""
        {
          "code": "welcome",
          "name": "欢迎邮件",
          "channelType": "EMAIL",
          "localizedTemplates": {
            "zh-CN": { "title": "欢迎", "content": "欢迎 {$name}" },
            "en": { "title": "Welcome", "content": "Welcome {$name}" }
          }
        }
        """);
  }

  public static TemplateEntity smsCaptchaZhCn() {
    return read("""
        {
          "code": "sms_captcha",
          "name": "短信验证码",
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "title": "验证码", "content": "您好 {{name}}，您的验证码是 {{code}}" }
          }
        }
        """);
  }

  public static TemplateEntity localizedEmpty() {
    return read("""
        {
          "code": "x",
          "name": "空本地化",
          "channelType": "SMS",
          "localizedTemplates": {}
        }
        """);
  }

  public static TemplateEntity localizedTemplatesNull() {
    return read("""
        {
          "code": "x",
          "name": "空本地化",
          "channelType": "SMS",
          "localizedTemplates": null
        }
        """);
  }

  public static TemplateEntity greetEn() {
    return read("""
        {
          "code": "greet",
          "name": "问候",
          "channelType": "SMS",
          "localizedTemplates": {
            "en": { "title": "t", "content": "Hello {{name}}" }
          }
        }
        """);
  }

  public static TemplateEntity emptyBodyDe() {
    return read("""
        {
          "code": "empty_body",
          "name": "空正文",
          "channelType": "SMS",
          "localizedTemplates": {
            "de": { "title": "only title", "content": null }
          }
        }
        """);
  }

  public static TemplateEntity invZhCn() {
    return read("""
        {
          "code": "inv",
          "name": "库存",
          "channelType": "SMS",
          "localizedTemplates": {
            "zh-CN": { "title": "t", "content": "x {{p}}" }
          }
        }
        """);
  }

  public static TemplateEntity brandWelcomeEmail() {
    return read("""
        {
          "code": "brand_welcome",
          "name": "品牌欢迎邮件",
          "channelType": "EMAIL",
          "localizedTemplates": {
            "en": {
              "title": "Welcome to {{brandName}} from {{teamName}}",
              "content": "Hello {{name}}, {{brandName}} is ready"
            }
          }
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
