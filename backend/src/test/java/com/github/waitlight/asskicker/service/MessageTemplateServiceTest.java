package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.repository.MessageTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
                AssKickerTestApplication.class,
                MongoTestConfiguration.class
}, properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class MessageTemplateServiceTest {

        @Autowired
        private MessageTemplateService messageTemplateService;

        @Autowired
        private MessageTemplateRepository messageTemplateRepository;

        @BeforeEach
        void clearTemplates() {
                StepVerifier.create(messageTemplateRepository.deleteAll()).verifyComplete();
        }

        @Test
        void create_smsCaptcha_findById_and_findByCodeAndChannelType_returnsFullPayload() {
                MessageTemplateEntity input = MessageTemplateEntityFixtures.smsCaptcha();

                StepVerifier.create(messageTemplateService.create(input)
                                .flatMap(saved -> messageTemplateService.findById(saved.getId())
                                                .zipWith(messageTemplateService.findByCodeAndChannelType(
                                                                saved.getCode(), saved.getChannelType()))))
                                .assertNext(tuple -> {
                                        MessageTemplateEntity byId = tuple.getT1();
                                        MessageTemplateEntity byKey = tuple.getT2();
                                        assertThat(byId.getId()).isNotBlank();
                                        assertThat(byId.getCreatedAt()).isNotNull();
                                        assertThat(byId.getUpdatedAt()).isNotNull();
                                        assertTemplateContentEqual(input, byId);
                                        assertThat(byKey.getId()).isEqualTo(byId.getId());
                                        assertTemplateContentEqual(input, byKey);
                                })
                                .verifyComplete();
        }

        @Test
        void findById_unknownId_completesEmpty() {
                StepVerifier.create(messageTemplateService.findById("507f1f77bcf86cd799439011"))
                                .verifyComplete();
        }

        @Test
        void findByChannelType_im_onlyReturnsImTemplates() {
                MessageTemplateEntity sms = MessageTemplateEntityFixtures.smsCaptcha();
                MessageTemplateEntity email = MessageTemplateEntityFixtures.emailCaptcha();
                MessageTemplateEntity im = MessageTemplateEntityFixtures.imOpsAlert();

                StepVerifier.create(messageTemplateService.create(sms)
                                .then(messageTemplateService.create(email))
                                .then(messageTemplateService.create(im))
                                .thenMany(messageTemplateService.findByChannelType(ChannelType.IM).collectList()))
                                .assertNext(list -> {
                                        assertThat(list).hasSize(1);
                                        MessageTemplateEntity e = list.get(0);
                                        assertThat(e.getChannelType()).isEqualTo(ChannelType.IM);
                                        assertThat(e.getCode()).isEqualTo(im.getCode());
                                        assertTemplateContentEqual(im, e);
                                })
                                .verifyComplete();
        }

        @Test
        void create_sameCodeAndChannelType_conflicts() {
                MessageTemplateEntity first = MessageTemplateEntityFixtures.smsCaptcha();
                MessageTemplateEntity second = MessageTemplateEntityFixtures.smsCaptcha();

                StepVerifier.create(messageTemplateService.create(first)
                                .then(messageTemplateService.create(second)))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(ResponseStatusException.class)
                                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                                .isEqualTo(HttpStatus.CONFLICT))
                                .verify();
        }

        @Test
        void create_sameCodeDifferentType_findByCodeAndChannelType_returnsEachTemplate() {
                MessageTemplateEntity sms = MessageTemplateEntityFixtures.smsCaptcha();
                MessageTemplateEntity email = MessageTemplateEntityFixtures.emailCaptcha();

                StepVerifier.create(messageTemplateService.create(sms)
                                .then(messageTemplateService.create(email))
                                .thenMany(Flux.zip(
                                                messageTemplateService.findByCodeAndChannelType("captcha",
                                                                ChannelType.SMS),
                                                messageTemplateService.findByCodeAndChannelType("captcha",
                                                                ChannelType.EMAIL))))
                                .assertNext(tuple -> {
                                        assertTemplateContentEqual(sms, tuple.getT1());
                                        assertTemplateContentEqual(email, tuple.getT2());
                                })
                                .verifyComplete();
        }

        @Test
        void update_pushTemplate_replacesPayloadAndRefreshesUpdatedAt() {
                MessageTemplateEntity input = MessageTemplateEntityFixtures.pushNewMessage();

                StepVerifier.create(messageTemplateService.create(input)
                                .flatMap(saved -> {
                                        assertThat(saved.getId()).isNotBlank();
                                        long before = saved.getUpdatedAt();

                                        ObjectNode templates = (ObjectNode) saved.getTemplates().deepCopy();
                                        ((ObjectNode) templates.get("zh-cn")).put("body", "patched-zh-body");

                                        MessageTemplateEntity patch = new MessageTemplateEntity();
                                        patch.setCode(saved.getCode());
                                        patch.setChannelType(saved.getChannelType());
                                        patch.setTemplates(templates);
                                        patch.setChannels(saved.getChannels());

                                        return Mono.delay(Duration.ofMillis(2))
                                                        .then(messageTemplateService.update(saved.getId(), patch))
                                                        .flatMap(updated -> messageTemplateService
                                                                        .findById(updated.getId()))
                                                        .map(reloaded -> new UpdatedPushAssert(before, input,
                                                                        reloaded));
                                }))
                                .assertNext(u -> {
                                        assertThat(u.reloaded().getUpdatedAt())
                                                        .isGreaterThanOrEqualTo(u.beforeUpdatedAt());
                                        assertThat(u.reloaded().getTemplates().path("zh-cn").path("body").asText())
                                                        .isEqualTo("patched-zh-body");
                                        assertThat(u.reloaded().getTemplates().path("en"))
                                                        .isEqualTo(u.input().getTemplates().path("en"));
                                        assertThat(u.reloaded().getChannels()).isEqualTo(u.input().getChannels());
                                })
                                .verifyComplete();
        }

        private record UpdatedPushAssert(long beforeUpdatedAt, MessageTemplateEntity input,
                        MessageTemplateEntity reloaded) {
        }

        @Test
        void update_welcomeEmail_toCaptchaSms_whenSmsExists_conflicts() {
                MessageTemplateEntity sms = MessageTemplateEntityFixtures.smsCaptcha();
                MessageTemplateEntity welcome = MessageTemplateEntityFixtures.welcomeEmail();

                StepVerifier.create(messageTemplateService.create(sms)
                                .then(messageTemplateService.create(welcome))
                                .then(messageTemplateService.findByCodeAndChannelType("welcome", ChannelType.EMAIL)
                                                .flatMap(w -> {
                                                        MessageTemplateEntity patch = new MessageTemplateEntity();
                                                        patch.setCode("captcha");
                                                        patch.setChannelType(ChannelType.SMS);
                                                        patch.setTemplates(w.getTemplates());
                                                        patch.setChannels(w.getChannels());
                                                        return messageTemplateService.update(w.getId(), patch);
                                                })))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(ResponseStatusException.class)
                                                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                                                .isEqualTo(HttpStatus.CONFLICT))
                                .verify();
        }

        @Test
        void delete_emailTemplate_removesDocument() {
                MessageTemplateEntity email = MessageTemplateEntityFixtures.emailCaptcha();

                StepVerifier.create(messageTemplateService.create(email)
                                .flatMap(saved -> messageTemplateService.delete(saved.getId())
                                                .then(messageTemplateService.findById(saved.getId()))))
                                .verifyComplete();
        }

        @Test
        void findAll_invalidPageOrSize_returnsEmpty() {
                StepVerifier.create(messageTemplateService.findAll(-1, 10).collectList())
                                .assertNext(list -> assertThat(list).isEmpty())
                                .verifyComplete();

                StepVerifier.create(messageTemplateService.findAll(0, 0).collectList())
                                .assertNext(list -> assertThat(list).isEmpty())
                                .verifyComplete();
        }

        @Test
        void findAll_pagination_returnsExpectedCounts() {
                MessageTemplateEntity a = MessageTemplateEntityFixtures.smsCaptcha();
                MessageTemplateEntity b = MessageTemplateEntityFixtures.imOpsAlert();
                MessageTemplateEntity c = MessageTemplateEntityFixtures.pushNewMessage();

                StepVerifier.create(messageTemplateService.create(a)
                                .then(messageTemplateService.create(b))
                                .then(messageTemplateService.create(c))
                                .thenMany(Flux.zip(
                                                messageTemplateService.findAll(0, 2).collectList(),
                                                messageTemplateService.findAll(1, 2).collectList())))
                                .assertNext(tuple -> {
                                        assertThat(tuple.getT1()).hasSize(2);
                                        assertThat(tuple.getT2()).hasSize(1);
                                })
                                .verifyComplete();
        }

        private static void assertTemplateContentEqual(MessageTemplateEntity expected, MessageTemplateEntity actual) {
                assertThat(actual.getCode()).isEqualTo(expected.getCode());
                assertThat(actual.getChannelType()).isEqualTo(expected.getChannelType());
                assertThat(actual.getTemplates()).isEqualTo(expected.getTemplates());
                assertThat(actual.getChannels()).isEqualTo(expected.getChannels());
        }
}
