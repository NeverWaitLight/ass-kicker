package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channel 服务层集成测试。
 *
 * 使用 {@link TestMethodOrder} 和 {@link Order} 确保测试按顺序执行，
 * 防止在共享数据库环境下测试之间的数据竞争。
 */
@SpringBootTest(classes = {
        AssKickerTestApplication.class,
        MongoTestConfiguration.class
}, properties = {
        "spring.main.web-application-type=none",
        "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChannelServiceTest {

    @Autowired
    private ChannelService channelService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // 在每个测试开始前清除所有数据（不依赖软删除条件）
        clearAllData();
    }

    @AfterEach
    void tearDown() {
        // 在每个测试结束后也清除数据，确保不污染后续测试
        clearAllData();
    }

    /**
     * 清除集合中的所有数据，不依赖软删除条件。
     * 使用 ReactiveMongoTemplate 直接删除整个集合中的所有文档。
     */
    private void clearAllData() {
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), ChannelEntity.class)
                .block(Duration.ofSeconds(5));
    }

    @Test
    @Order(1)
    void create_smsAliyun_findByKey_returnsFullConfig() {
        ChannelEntity input = ChannelEntityFixtures.smsAliyun();

        StepVerifier.create(channelService.create(input)
                .flatMap(saved -> channelService.findByKey(saved.getCode())))
                .assertNext(found -> {
                    assertThat(found.getId()).isNotBlank();
                    assertThat(found.getCreatedAt()).isNotNull();
                    assertThat(found.getUpdatedAt()).isNotNull();
                    assertChannelContentEqual(input, found);
                })
                .verifyComplete();
    }

    @Test
    @Order(2)
    void findByType_email_onlyReturnsEmailChannels() {
        ChannelEntity sms = ChannelEntityFixtures.smsAliyun();
        ChannelEntity email = ChannelEntityFixtures.emailSmtp();
        ChannelEntity im = ChannelEntityFixtures.imSlackEnabled();
        ChannelEntity push = ChannelEntityFixtures.pushApnsEnabled();

        StepVerifier.create(Flux.concat(
                channelService.create(sms),
                channelService.create(email),
                channelService.create(im),
                channelService.create(push))
                .then(channelService.findByType(ChannelType.EMAIL).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelEntity e = list.get(0);
                    assertThat(e.getChannelType()).isEqualTo(ChannelType.EMAIL);
                    assertThat(e.getCode()).isEqualTo(email.getCode());
                    assertChannelContentEqual(email, e);
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    void findEnabledByType_im_onlyReturnsEnabledImChannels() {
        ChannelEntity on = ChannelEntityFixtures.imSlackEnabled();
        ChannelEntity off = ChannelEntityFixtures.imSlackDisabled();

        StepVerifier.create(Flux.concat(
                channelService.create(on),
                channelService.create(off))
                .then(channelService.findEnabledByType(ChannelType.IM).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelEntity e = list.get(0);
                    assertThat(e.getCode()).isEqualTo(on.getCode());
                    assertThat(e.isEnabled()).isTrue();
                    assertChannelContentEqual(on, e);
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    void findEnabledByType_push_excludesDisabled() {
        ChannelEntity on = ChannelEntityFixtures.pushApnsEnabled();
        ChannelEntity off = ChannelEntityFixtures.pushApnsDisabled();

        StepVerifier.create(Flux.concat(
                channelService.create(on),
                channelService.create(off))
                .then(channelService.findEnabledByType(ChannelType.PUSH).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelEntity e = list.get(0);
                    assertThat(e.getCode()).isEqualTo(on.getCode());
                    assertThat(e.isEnabled()).isTrue();
                    assertChannelContentEqual(on, e);
                })
                .verifyComplete();
    }

    private static void assertChannelContentEqual(ChannelEntity expected, ChannelEntity actual) {
        assertThat(actual.getCode()).isEqualTo(expected.getCode());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getChannelType()).isEqualTo(expected.getChannelType());
        assertThat(actual.getProviderType()).isEqualTo(expected.getProviderType());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getPriorityAddressRegex()).isEqualTo(expected.getPriorityAddressRegex());
        assertThat(actual.getExcludeAddressRegex()).isEqualTo(expected.getExcludeAddressRegex());
        assertThat(actual.isEnabled()).isEqualTo(expected.isEnabled());
        assertThat(actual.getRateLimit()).usingRecursiveComparison().isEqualTo(expected.getRateLimit());
        assertThat(actual.getProperties()).isEqualTo(expected.getProperties());
    }
}
