package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;

import java.io.UncheckedIOException;

/**
 * 文档 {@code docs/message-template-and-channel-provider.md} 中 Template Config
 * 样例，
 * 用于 MessageTemplateService 嵌入式 Mongo 测试。
 */
public final class MessageTemplateEntityFixtures {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MessageTemplateEntityFixtures() {
  }

  public static MessageTemplateEntity smsCaptcha() {
    return read("""
        {
          "code": "captcha",
          "channelType": "SMS",
          "channels": {
            "aliyun": {
              "templateCode": "SMS_123456789"
            },
            "tencent": {
              "templateId": "1234567"
            }
          },
          "templates": {
            "zh-cn": {
              "content": "{$channel.signName}这是登录验证码 {$code}"
            },
            "en": {
              "content": "{$channel.signName}This is your captcha {$code}"
            }
          }
        }
        """);
  }

  public static MessageTemplateEntity emailCaptcha() {
    return read("""
        {
          "code": "captcha_email",
          "channelType": "EMAIL",
          "templates": {
            "zh-cn": {
              "subject": "验证码",
              "content": "这是你的验证码 {$code}"
            },
            "en": {
              "subject": "Verification code",
              "content": "This is your verification code {$code}"
            }
          }
        }
        """);
  }

  public static MessageTemplateEntity imOpsAlert() {
    return read("""
        {
          "code": "ops_alert",
          "channelType": "IM",
          "channels": {
            "slack": {
              "text": "{$template.title}: {$template.body}"
            },
            "telegram": {
              "text": "<b>{$template.title}</b>\\n{$template.body}",
              "parse_mode": "HTML"
            }
          },
          "templates": {
            "zh-cn": {
              "title": "告警",
              "body": "服务 {$service} 出现异常"
            },
            "en": {
              "title": "Alert",
              "body": "Service {$service} has an issue"
            }
          }
        }
        """);
  }

  public static MessageTemplateEntity pushNewMessage() {
    return read("""
        {
          "code": "new_message_push",
          "channelType": "PUSH",
          "channels": {
            "apns": {
              "aps": {
                "alert": {
                  "title": "{$title}",
                  "body": "{$body}"
                },
                "sound": "default",
                "badge": 1
              },
              "chatId": "{$chatId}",
              "messageId": "{$msgId}"
            },
            "fcm": {
              "message": {
                "notification": {
                  "title": "{$title}",
                  "body": "{$body}"
                },
                "data": {
                  "chatId": "{$chatId}",
                  "messageId": "{$msgId}"
                }
              }
            }
          },
          "templates": {
            "zh-cn": {
              "title": "新消息",
              "body": "对方昵称:消息摘要..."
            },
            "en": {
              "title": "New Message",
              "body": "nickname:shour message..."
            }
          }
        }
        """);
  }

  /** 与 {@link #smsCaptcha()} 同 code 不同 channelType，用于更新冲突场景。 */
  public static MessageTemplateEntity welcomeEmail() {
    return read("""
        {
          "code": "welcome",
          "channelType": "EMAIL",
          "templates": {
            "zh-cn": {
              "subject": "欢迎",
              "content": "欢迎 {$name}"
            }
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
