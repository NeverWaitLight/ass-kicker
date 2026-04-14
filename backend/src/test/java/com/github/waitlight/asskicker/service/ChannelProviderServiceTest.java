package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
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
 * ChannelProvider 服务层集成测试。
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
class ChannelProviderServiceTest {

    @Autowired
    private ChannelProviderService channelProviderService;

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
        mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), ChannelProviderEntity.class)
                .block(Duration.ofSeconds(5));
    }

    @Test
    @Order(1)
    void create_smsAliyun_findByKey_returnsFullConfig() {
        ChannelProviderEntity input = ChannelProviderEntityFixtures.smsAliyun();

        StepVerifier.create(channelProviderService.create(input)
                .flatMap(saved -> channelProviderService.findByKey(saved.getCode())))
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
        ChannelProviderEntity sms = ChannelProviderEntityFixtures.smsAliyun();
        ChannelProviderEntity email = ChannelProviderEntityFixtures.emailSmtp();
        ChannelProviderEntity im = ChannelProviderEntityFixtures.imSlackEnabled();
        ChannelProviderEntity push = ChannelProviderEntityFixtures.pushApnsEnabled();

        StepVerifier.create(Flux.concat(
                channelProviderService.create(sms),
                channelProviderService.create(email),
                channelProviderService.create(im),
                channelProviderService.create(push))
                .then(channelProviderService.findByType(ChannelType.EMAIL).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelProviderEntity e = list.get(0);
                    assertThat(e.getChannelType()).isEqualTo(ChannelType.EMAIL);
                    assertThat(e.getCode()).isEqualTo(email.getCode());
                    assertChannelContentEqual(email, e);
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    void findEnabledByType_im_onlyReturnsEnabledImChannels() {
        ChannelProviderEntity on = ChannelProviderEntityFixtures.imSlackEnabled();
        ChannelProviderEntity off = ChannelProviderEntityFixtures.imSlackDisabled();

        StepVerifier.create(Flux.concat(
                channelProviderService.create(on),
                channelProviderService.create(off))
                .then(channelProviderService.findEnabledByType(ChannelType.IM).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelProviderEntity e = list.get(0);
                    assertThat(e.getCode()).isEqualTo(on.getCode());
                    assertThat(e.isEnabled()).isTrue();
                    assertChannelContentEqual(on, e);
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    void findEnabledByType_push_excludesDisabled() {
        ChannelProviderEntity on = ChannelProviderEntityFixtures.pushApnsEnabled();
        ChannelProviderEntity off = ChannelProviderEntityFixtures.pushApnsDisabled();

        StepVerifier.create(Flux.concat(
                channelProviderService.create(on),
                channelProviderService.create(off))
                .then(channelProviderService.findEnabledByType(ChannelType.PUSH).collectList()))
                .assertNext(list -> {
                    assertThat(list).hasSize(1);
                    ChannelProviderEntity e = list.get(0);
                    assertThat(e.getCode()).isEqualTo(on.getCode());
                    assertThat(e.isEnabled()).isTrue();
                    assertChannelContentEqual(on, e);
                })
                .verifyComplete();
    }

    private static void assertChannelContentEqual(ChannelProviderEntity expected, ChannelProviderEntity actual) {
        assertThat(actual.getCode()).isEqualTo(expected.getCode());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getChannelType()).isEqualTo(expected.getChannelType());
        assertThat(actual.getProviderType()).isEqualTo(expected.getProviderType());
        assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
        assertThat(actual.getPriorityAddressRegex()).isEqualTo(expected.getPriorityAddressRegex());
        assertThat(actual.getExcludeAddressRegex()).isEqualTo(expected.getExcludeAddressRegex());
        assertThat(actual.isEnabled()).isEqualTo(expected.isEnabled());
        assertThat(actual.getProperties()).isEqualTo(expected.getProperties());
    }
}