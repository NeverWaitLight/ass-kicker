package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        AssKickerTestApplication.class,
        MongoTestConfiguration.class
}, properties = {
        "spring.main.web-application-type=none",
        "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class ChannelProviderServiceTest {

    @Autowired
    private ChannelProviderService channelProviderService;

    @Autowired
    private ChannelProviderRepository channelProviderRepository;

    @BeforeEach
    void clearProviders() {
        StepVerifier.create(channelProviderRepository.deleteAll()).verifyComplete();
    }

    @Test
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
