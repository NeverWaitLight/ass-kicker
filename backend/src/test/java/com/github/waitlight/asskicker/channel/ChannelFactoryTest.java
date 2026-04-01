package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelProviderType;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

class ChannelFactoryTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ChannelFactory factory = new ChannelFactory(WebClient.create(),
          ChannelTestObjectMappers.channelObjectMapper());

  @Test
  @DisplayName("创建APNS渠道")
  void create_apns_returnsApnsChannel() throws Exception {
    String json = """
        {
          "code": "apns-factory",
          "channelType": "PUSH",
          "providerType": "APNS",
          "enabled": true,
          "properties": {
            "url": "https://api.sandbox.push.apple.com",
            "bundleIdTopic": "com.example.app",
            "teamId": "TEST_TEAM_ID",
            "keyId": "TEST_KEY_ID",
            "privateKeyPem": "-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBNZQdW2XALI6odi\\nsffzbONZ5+i8V1xxzKs88K2KPhShRANCAARfjzU68VEgfLL0eZ38qjls03GvRFwJ\\nNQLOBe4rsFB6lqOYiNME6oCVt4o5Ju46ca2RWappiw8v21uMLZPTLjx5\\n-----END PRIVATE KEY-----\\n"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(ApnsChannel.class);
  }

  @Test
  @DisplayName("创建FCM渠道")
  void create_fcm_returnsFcmChannel() throws Exception {
    String json = """
        {
          "code": "fcm-factory",
          "channelType": "PUSH",
          "providerType": "FCM",
          "enabled": true,
          "properties": {
            "url": "https://fcm.googleapis.com/v1/projects/demo/messages:send",
            "projectId": "demo",
            "accessToken": "test-access-token"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(FcmChannel.class);
  }

  @Test
  @DisplayName("创建钉钉Webhook渠道")
  void create_dingtalk_returnsDingtalkWebhookChannel() throws Exception {
    String json = """
        {
          "code": "dingtalk-factory",
          "channelType": "IM",
          "providerType": "DINGTALK",
          "enabled": true,
          "properties": {
            "url": "https://oapi.dingtalk.com/robot/send"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(DingtalkWebhookChannel.class);
  }

  @Test
  @DisplayName("创建企业微信Webhook渠道")
  void create_wecom_returnsWecomWebhookChannel() throws Exception {
    String json = """
        {
          "code": "wecom-factory",
          "channelType": "IM",
          "providerType": "WECOM",
          "enabled": true,
          "properties": {
            "url": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(WecomWebhookChannel.class);
  }

  @Test
  @DisplayName("创建飞书Webhook渠道")
  void create_feishu_returnsFeishuWebhookChannel() throws Exception {
    String json = """
        {
          "code": "feishu-factory",
          "channelType": "IM",
          "providerType": "FEISHU",
          "enabled": true,
          "properties": {
            "url": "https://open.feishu.cn/open-apis/bot/v2/hook"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(FeishuWebhookChannel.class);
  }

  @Test
  @DisplayName("创建钉钉Bot渠道")
  void create_dingtalkBot_returnsDingtalkBotChannel() throws Exception {
    String json = """
        {
          "code": "dingtalk-bot-factory",
          "channelType": "IM",
          "providerType": "DINGTALK_BOT",
          "enabled": true,
          "properties": {
            "appKey": "k",
            "appSecret": "s",
            "robotCode": "r"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(DingtalkBotChannel.class);
  }

  @Test
  @DisplayName("创建企业微信Bot渠道")
  void create_wecomBot_returnsWecomBotChannel() throws Exception {
    String json = """
        {
          "code": "wecom-bot-factory",
          "channelType": "IM",
          "providerType": "WECOM_BOT",
          "enabled": true,
          "properties": {
            "corpId": "ww",
            "corpSecret": "sec"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(WecomBotChannel.class);
  }

  @Test
  @DisplayName("创建飞书Bot渠道")
  void create_feishuBot_returnsFeishuBotChannel() throws Exception {
    String json = """
        {
          "code": "feishu-bot-factory",
          "channelType": "IM",
          "providerType": "FEISHU_BOT",
          "enabled": true,
          "properties": {
            "appId": "cli_x",
            "appSecret": "sec_x"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(FeishuBotChannel.class);
  }

  @Test
  @DisplayName("创建不支持的渠道")
  void create_unsupportedProvider_returnsNull() throws Exception {
    String json = """
        {
          "code": "slack-factory",
          "channelType": "IM",
          "providerType": "SLACK",
          "enabled": true,
          "properties": {}
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isNull();
  }

  @Test
  @DisplayName("创建阿里云短信渠道")
  void create_aliyunSms_returnsAliyunSmsChannel() throws Exception {
    String json = """
        {
          "code": "aliyun-sms-factory",
          "channelType": "SMS",
          "providerType": "ALIYUN_SMS",
          "enabled": true,
          "properties": {
            "accessKeyId": "a",
            "accessKeySecret": "s",
            "signName": "sig",
            "templateCode": "SMS_1",
            "endpoint": "dysmsapi.aliyuncs.com"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(AliyunSmsChannel.class);
  }

  @Test
  @DisplayName("创建 AWS SNS 短信渠道")
  void create_awsSms_returnsAwsSnsSmsChannel() throws Exception {
    String json = """
        {
          "code": "aws-sms-factory",
          "channelType": "SMS",
          "providerType": "AWS_SMS",
          "enabled": true,
          "properties": {
            "accessKeyId": "AKIA",
            "secretAccessKey": "sec",
            "region": "us-east-1"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(AwsSnsSmsChannel.class);
  }

  @Test
  @DisplayName("创建 SMTP 邮件渠道")
  void create_smtp_returnsSmtpChannel() throws Exception {
    String json = """
        {
          "code": "smtp-factory",
          "channelType": "EMAIL",
          "providerType": "SMTP",
          "enabled": true,
          "properties": {
            "host": "localhost",
            "port": "25",
            "username": "u",
            "password": "p",
            "from": "a@b.c"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(SmtpChannel.class);
  }

  @Test
  @DisplayName("返回支持的渠道类型列表")
  void getSupportedTypes_containsWebhookProviders() {
    assertThat(factory.getSupportedTypes()).containsExactly(
        ChannelProviderType.ALIYUN_SMS,
        ChannelProviderType.AWS_SMS,
        ChannelProviderType.ALIYUN_EMAIL,
        ChannelProviderType.AWS_EMAIL,
        ChannelProviderType.SMTP,
        ChannelProviderType.APNS,
        ChannelProviderType.FCM,
        ChannelProviderType.DINGTALK,
        ChannelProviderType.WECOM,
        ChannelProviderType.FEISHU,
        ChannelProviderType.DINGTALK_BOT,
        ChannelProviderType.WECOM_BOT,
        ChannelProviderType.FEISHU_BOT);
    assertThat(factory.getSupportedTypes()).containsAll(List.of(
        ChannelProviderType.DINGTALK,
        ChannelProviderType.WECOM,
        ChannelProviderType.FEISHU,
        ChannelProviderType.DINGTALK_BOT,
        ChannelProviderType.WECOM_BOT,
        ChannelProviderType.FEISHU_BOT));
  }
}