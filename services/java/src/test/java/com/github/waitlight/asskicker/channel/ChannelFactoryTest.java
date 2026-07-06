package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.waitlight.asskicker.channel.impl.*;
import com.github.waitlight.asskicker.model.ChannelEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class ChannelFactoryTest {

  private static final ObjectMapper MAPPER = new ObjectMapper()
          .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

  private final ChannelFactory factory = new ChannelFactory(WebClient.create(),
          ChannelTestObjectMappers.channelObjectMapper());

  @BeforeEach
  void scan() {
    factory.scanChannelImplementations();
  }

  @Test
  @DisplayName("创建APNS渠道")
  void create_apns_returnsApnsChannel() throws Exception {
    String json = """
        {
          "code": "apns-factory",
          "type": "APNS",
          "provider": "APPLE",
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
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(ApnsPushChannel.class);
  }

  @Test
  @DisplayName("创建FCM渠道")
  void create_fcm_returnsFcmChannel() throws Exception {
    String testPrivateKey = "-----BEGIN PRIVATE KEY-----\\n"
            + "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDC8AJnama5pcR6\\n"
            + "ZAESNbsnw/buaGlc2sHDasdgjRlzx6Brtiw+mVfIVA/6BrtGIYmDRi6MCrCBlwJa\\n"
            + "bQgGdD6IIkh4aqWaaO1f4qKC6QYUVNKsbDQDVa9YTQ7xwtifImCrtXWRWqlGt1FH\\n"
            + "OlDwJjGyjv2EnSkRSNd42dCjqAQApNnXdziLTnw0skkz59Bps8Wf5cZc8vCnO7nT\\n"
            + "ZSupUCi/3wkiadX0LetpyiAEhPCsj99TsZrGQaTVoBxiupqqRS8aoao8xSWSCXXf\\n"
            + "P9YqPjWkQjF4qYuMsqjQ6uH24M+biAycT7zshlVF/K37b1DBwl/JLwL2mB7TP8Ob\\n"
            + "x46Gel4FAgMBAAECggEADQNGOHlnARaP6UGlZqif5TV/gDEHyN3bgxGCMgTz7Cwp\\n"
            + "HneV4tnhUmtLeREboutYiXhVDB58jjIH7gRgPZoN6zrVTqCD4dew8i59Uwgo0pD0\\n"
            + "/UiTJ55/1YD+Bzp5XmobkkAgky2TpjeqMnsx7aNcZu3aU5DFgzSzGYtPZ6quwGIi\\n"
            + "4tZEMXCwireuaJacUDftJgmbsfMZQTaHzmVbuNEtcUwPKVvrTDVDpmPUsdFlJURb\\n"
            + "2ax1aPd0SAq0n9/w1fl+LWLeI+lizdCx1moPxAtLTA/shGax36aLIgSdWo9Tgxiw\\n"
            + "FLzwiAmgupm0wx5YYy9gqZ4kwuAufnSfMvu7WAGzMwKBgQDv1EeGk9HJDiZC9ixM\\n"
            + "dq8QNtSHFRmMccRUA6B6YiCzbFrBBGl4Fmks2Xkb4AeNMpzkj63zacZHPhPuBaY5\\n"
            + "R+DyWmbACnVYGC7kk2MssVm1GhsI2G8WbpN7Mks0Lef6Ve1s5iTk+ttcKCRhoRNe\\n"
            + "A4IXCZmXLIfrkhtQh9jcdjT7bwKBgQDQFNlwqPR+cT/vj1omUrfG5jMdWPK1FvZt\\n"
            + "GeisWjgJCH0bKWrMjXXzUS2jU/h/7OeSXE4P9sJF6ZumJGcNSpNajXljsJG/dZKI\\n"
            + "2BVLooOKLwygZmHSsdKDxZdMijZW6PXtsu2hFgvK54kfqpa6St7LeXuD26QeOrBk\\n"
            + "eMonAS5TywKBgQC5VS1U5WSH36RXuM8w48KTYBvKq9aLfts+JXNdP/mPThuv703l\\n"
            + "3EO4wfJiRTTwu30c7594bHQqV+Gk3b6/ozlFb/DZVPurcTzDrNZGEmOFnT/pDQCD\\n"
            + "sD3ORWZyU0tiXAbXUd6PCQB9bhP3UjeaPlHIpcWIWoRK2iS7jc9bRwnYhwKBgFzG\\n"
            + "/xCZdLIwAqbozvRJa4G2wFG0iDswKt4IcFLwww1cCJQkyma8KDw+FNA/L4yyb6o1\\n"
            + "l+TMTGDpwSm6D2zAtKTqcZZ+cu3gGV8EobIgmu/w/HtESxeri8aPQl+xPHtR0d2T\\n"
            + "Kxro/ocQ53YEFMKpgV9OIkFvnGSSHHYf2Vq8zxAzAoGAX4LEygHiIg2p8Bkn4z1E\\n"
            + "dGhDB1IDR5lMLPlnw1DIi5r1dEW28vRrtGlR1oYGr0+13t1F9wJtW2IPz9HIurhw\\n"
            + "faY6i1BxkidmA/QNY+cqE7TzM3EDtpnDKX301VvjaUcbY2FtoxKTI/HO+R1md/Mu\\n"
            + "sucBLezQ8+gwpC+rDMtGCCA=\\n"
            + "-----END PRIVATE KEY-----\\n";
    String serviceAccountJson = "{\\\"type\\\":\\\"service_account\\\",\\\"project_id\\\":\\\"demo\\\","
            + "\\\"private_key_id\\\":\\\"id\\\",\\\"private_key\\\":\\\"" + testPrivateKey + "\\\","
            + "\\\"client_email\\\":\\\"x@y.iam.gserviceaccount.com\\\",\\\"client_id\\\":\\\"1\\\","
            + "\\\"token_uri\\\":\\\"https://oauth2.googleapis.com/token\\\"}";
    String json = """
        {
          "code": "fcm-factory",
          "type": "FCM",
          "provider": "GOOGLE",
          "enabled": true,
          "properties": {
            "projectId": "demo",
            "serviceAccountJson": "%s"
          }
        }
        """.formatted(serviceAccountJson);
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(FcmPushChannel.class);
  }

  @Test
  @DisplayName("创建钉钉Bot渠道")
  void create_dingtalkBot_returnsDingtalkBotChannel() throws Exception {
    String json = """
        {
          "code": "dingtalk-bot-factory",
          "type": "DINGTALK",
          "provider": "DINGTALK",
          "enabled": true,
          "properties": {}
        }
        """;
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(DingTalkImChannel.class);
  }

  @Test
  @DisplayName("创建飞书Bot渠道")
  void create_feishuBot_returnsFeiShuImChannel() throws Exception {
    String json = """
        {
          "code": "feishu-bot-factory",
          "type": "FEISHU",
          "provider": "FEISHU",
          "enabled": true,
          "properties": {}
        }
        """;
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(FeiShuImChannel.class);
  }

  @Test
  @DisplayName("创建不支持的渠道")
  void create_unsupportedProvider_returnsNull() throws Exception {
    String json = """
        {
          "code": "slack-factory",
          "type": "DINGTALK",
          "provider": "SLACK",
          "enabled": true,
          "properties": {}
        }
        """;
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isNull();
  }

  @Test
  @DisplayName("创建阿里云短信渠道")
  void create_aliyunSms_returnsAliyunSmsChannel() throws Exception {
    String json = """
        {
          "code": "aliyun-sms-factory",
          "type": "SMS",
          "provider": "ALIYUN",
          "enabled": true,
          "properties": {
            "accessKeyId": "a",
            "accessKeySecret": "s",
            "endpoint": "dysmsapi.aliyuncs.com"
          }
        }
        """;
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(AliyunSmsChannel.class);
  }

  @Test
  @DisplayName("创建腾讯云短信渠道")
  void create_tencentSms_returnsTencentSmsChannel() throws Exception {
    String json = """
        {
          "code": "tencent-sms-factory",
          "type": "SMS",
          "provider": "TENCENT",
          "enabled": true,
          "properties": {
            "secretId": "id",
            "secretKey": "key",
            "region": "ap-guangzhou",
            "endpoint": "sms.tencentcloudapi.com"
          }
        }
        """;
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(TencentSmsChannel.class);
  }

  @Test
  @DisplayName("创建 SMTP 邮件渠道")
  void create_smtp_returnsSmtpChannel() throws Exception {
    String json = """
        {
          "code": "smtp-factory",
          "type": "EMAIL",
          "provider": "SMTP",
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
    ChannelEntity entity = MAPPER.readValue(json, ChannelEntity.class);
    assertThat(factory.create(entity)).isInstanceOf(SmtpEmailChannel.class);
  }
}