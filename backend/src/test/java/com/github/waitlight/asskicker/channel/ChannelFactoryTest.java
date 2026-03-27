package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

class ChannelFactoryTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final ChannelFactory factory = new ChannelFactory();

  @Test
  @DisplayName("创建APNS渠道处理器")
  void create_apns_returnsApnsHandler() throws Exception {
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
    assertThat(factory.create(entity)).isInstanceOf(ApnsChannelHandler.class);
  }

  @Test
  @DisplayName("创建FCM渠道处理器")
  void create_fcm_returnsFcmHandler() throws Exception {
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
    assertThat(factory.create(entity)).isInstanceOf(FcmChannelHandler.class);
  }

  @Test
  @DisplayName("创建不支持的渠道处理器")
  void create_unsupportedProvider_returnsNull() throws Exception {
    String json = """
        {
          "code": "sms-factory",
          "channelType": "SMS",
          "providerType": "ALIYUN_SMS",
          "enabled": true,
          "properties": {
            "accessKeyId": "a",
            "accessKeySecret": "s"
          }
        }
        """;
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    assertThat(factory.create(entity)).isNull();
  }
}