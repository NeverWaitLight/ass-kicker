package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(classes = {
    AssKickerTestApplication.class,
    MongoTestConfiguration.class
}, properties = {
    "spring.main.web-application-type=none",
    "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class ChannelManagerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String APNS_PROPS = """
      "properties": {
        "url": "https://api.sandbox.push.apple.com",
        "bundleIdTopic": "com.example.app",
        "teamId": "TEST_TEAM_ID",
        "keyId": "TEST_KEY_ID",
        "privateKeyPem": "-----BEGIN PRIVATE KEY-----\\nMIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBNZQdW2XALI6odi\\nsffzbONZ5+i8V1xxzKs88K2KPhShRANCAARfjzU68VEgfLL0eZ38qjls03GvRFwJ\\nNQLOBe4rsFB6lqOYiNME6oCVt4o5Ju46ca2RWappiw8v21uMLZPTLjx5\\n-----END PRIVATE KEY-----\\n"
      }
      """;
  private static final String FCM_PROPS = """
      "properties": {
        "url": "https://fcm.googleapis.com",
        "projectId": "test-project",
        "accessToken": "test-token"
      }
      """;

  @Autowired
  private ChannelProviderRepository channelProviderRepository;

  @Autowired
  private ChannelManager channelManager;

  @BeforeEach
  void clearProviders() {
    StepVerifier.create(channelProviderRepository.deleteAll()).verifyComplete();
    channelManager.refresh();
  }

  // 验证初始无处理器 刷新后可从仓库加载并按ID获取对应处理器
  @Test
  void init_and_refresh_loadChannels() throws Exception {
    assertThat(channelManager.getChannelCount()).isZero();

    String json = String.format("""
        {
          "code": "apns-it",
          "channelType": "PUSH",
          "providerType": "APNS",
          "enabled": true,
          %s
        }
        """, APNS_PROPS);
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    StepVerifier.create(channelProviderRepository.save(entity))
        .assertNext(saved -> assertThat(saved.getId()).isNotBlank())
        .verifyComplete();

    channelManager.refresh();
    assertThat(channelManager.getChannelCount()).isEqualTo(1);
    assertThat(channelManager.getAllChannels()).hasSize(1);
    StepVerifier.create(channelProviderRepository.findAll().collectList())
        .assertNext(list -> {
          String id = list.get(0).getId();
          assertThat(channelManager.getChannelById(id))
              .isInstanceOf(ApnsChannel.class);
        })
        .verifyComplete();
  }

  // 验证选择处理器时优先级规则生效 且命中排除规则的候选会被跳过
  @Test
  void selectChannel_respectsPriorityAndExclude() throws Exception {
    String common = """
        "channelType": "PUSH",
        "providerType": "APNS",
        "enabled": true,
        """;

    String lowPriority = String.format("""
        {
          "code": "z-apns",
          %s
          "priorityAddressRegex": "^nomatch$",
          "excludeAddressRegex": "^$",
          %s
        }
        """, common, APNS_PROPS);

    String highPriority = String.format("""
        {
          "code": "a-apns",
          %s
          "priorityAddressRegex": "^token-",
          "excludeAddressRegex": "^$",
          %s
        }
        """, common, APNS_PROPS);

    String excluded = String.format("""
        {
          "code": "ex-apns",
          %s
          "priorityAddressRegex": ".*",
          "excludeAddressRegex": "^token-X",
          %s
        }
        """, common, APNS_PROPS);

    ChannelProviderEntity e1 = MAPPER.readValue(lowPriority, ChannelProviderEntity.class);
    ChannelProviderEntity e2 = MAPPER.readValue(highPriority, ChannelProviderEntity.class);
    ChannelProviderEntity e3 = MAPPER.readValue(excluded, ChannelProviderEntity.class);

    StepVerifier.create(channelProviderRepository.saveAll(Flux.just(e1, e2, e3)).then())
        .verifyComplete();

    channelManager.refresh();

    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "token-abc"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "token-X"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
  }

  // 验证当同一渠道同时配置 priority 与 exclude 时 exclude 先于 priority 生效
  @Test
  void selectChannel_whenBothPriorityAndExcludePresent_excludeWins() throws Exception {
    String common = """
        "channelType": "PUSH",
        "providerType": "APNS",
        "enabled": true,
        """;

    String both = String.format("""
        {
          "code": "a-both",
          %s
          "priorityAddressRegex": ".*",
          "excludeAddressRegex": "^match$",
          %s
        }
        """, common, APNS_PROPS);

    String fallback = String.format("""
        {
          "code": "b-fallback",
          "providerType": "FCM",
          "channelType": "PUSH",
          "enabled": true,
          %s
        }
        """, FCM_PROPS);

    ChannelProviderEntity e1 = MAPPER.readValue(both, ChannelProviderEntity.class);
    ChannelProviderEntity e2 = MAPPER.readValue(fallback, ChannelProviderEntity.class);
    StepVerifier.create(channelProviderRepository.saveAll(Flux.just(e1, e2)).then())
        .verifyComplete();
    channelManager.refresh();

    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "match"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(FcmChannel.class))
        .verifyComplete();
    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "other"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
  }

  // 验证仅配置 priority 与仅配置 exclude 的渠道并存时 选择器按规则返回处理器
  @Test
  void selectChannel_withPriorityOnlyAndExcludeOnly_selectsByRules() throws Exception {
    String common = """
        "channelType": "PUSH",
        "providerType": "APNS",
        "enabled": true,
        """;
    String priorityOnly = String.format("""
        {
          "code": "a-priority-only",
          %s
          "priorityAddressRegex": "^p-.*$",
          %s
        }
        """, common, APNS_PROPS);
    String excludeOnly = String.format("""
        {
          "code": "b-exclude-only",
          "providerType": "FCM",
          "channelType": "PUSH",
          "enabled": true,
          "excludeAddressRegex": "^x-.*$",
          %s
        }
        """, FCM_PROPS);

    ChannelProviderEntity e1 = MAPPER.readValue(priorityOnly, ChannelProviderEntity.class);
    ChannelProviderEntity e2 = MAPPER.readValue(excludeOnly, ChannelProviderEntity.class);
    StepVerifier.create(channelProviderRepository.saveAll(Flux.just(e1, e2)).then())
        .verifyComplete();
    channelManager.refresh();

    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "p-target"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "x-target"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "normal"))
        .assertNext(selected -> assertThat(selected).isInstanceOf(ApnsChannel.class))
        .verifyComplete();
  }

  // 验证当所有候选都被排除时 选择结果为空流
  @Test
  void selectChannel_whenAllCandidatesExcluded_returnsEmpty() throws Exception {
    String json = String.format("""
        {
          "code": "solo-exclude",
          "channelType": "PUSH",
          "providerType": "APNS",
          "enabled": true,
          "priorityAddressRegex": ".*",
          "excludeAddressRegex": "^solo$",
          %s
        }
        """, APNS_PROPS);
    ChannelProviderEntity entity = MAPPER.readValue(json, ChannelProviderEntity.class);
    StepVerifier.create(channelProviderRepository.save(entity))
        .assertNext(saved -> assertThat(saved.getCode()).isEqualTo("solo-exclude"))
        .verifyComplete();
    channelManager.refresh();

    StepVerifier.create(channelManager.chose(ChannelType.PUSH, "solo"))
        .verifyComplete();
  }

}
