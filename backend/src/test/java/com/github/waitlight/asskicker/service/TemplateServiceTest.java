package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.AssKickerTestApplication;
import com.github.waitlight.asskicker.config.MongoTestConfiguration;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity.LocalizedTemplate;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
                AssKickerTestApplication.class,
                MongoTestConfiguration.class
}, properties = {
                "spring.main.web-application-type=none",
                "de.flapdoodle.mongodb.embedded.version=7.0.14"
})
class TemplateServiceTest {

        @Autowired
        private TemplateService templateService;

        @Autowired
        private TemplateRepository templateRepository;

        @BeforeEach
        void clearTemplates() {
                StepVerifier.create(templateRepository.deleteAll()).verifyComplete();
        }

        @Test
        void create_smsCaptcha_findById_and_findByCodeAndChannelType_returnsFullPayload() {
                TemplateEntity input = TemplateEntityFixtures.smsCaptcha();

                StepVerifier.create(templateService.create(input)
                                .flatMap(saved -> templateService.findById(saved.getId())
                                                .zipWith(templateService.findByCodeAndChannelType(
                                                                saved.getCode(), saved.getChannelType()))))
                                .assertNext(tuple -> {
                                        TemplateEntity byId = tuple.getT1();
                                        TemplateEntity byKey = tuple.getT2();
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
        void findById_unknownId_throwsNotFoundException() {
                StepVerifier.create(templateService.findById("507f1f77bcf86cd799439011"))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(NotFoundException.class))
                                .verify();
        }

        @Test
        void findByChannelType_im_onlyReturnsImTemplates() {
                TemplateEntity sms = TemplateEntityFixtures.smsCaptcha();
                TemplateEntity email = TemplateEntityFixtures.emailCaptcha();
                TemplateEntity im = TemplateEntityFixtures.imOpsAlert();

                StepVerifier.create(templateService.create(sms)
                                .then(templateService.create(email))
                                .then(templateService.create(im))
                                .thenMany(templateService.findByChannelType(ChannelType.IM).collectList()))
                                .assertNext(list -> {
                                        assertThat(list).hasSize(1);
                                        TemplateEntity e = list.get(0);
                                        assertThat(e.getChannelType()).isEqualTo(ChannelType.IM);
                                        assertThat(e.getCode()).isEqualTo(im.getCode());
                                        assertTemplateContentEqual(im, e);
                                })
                                .verifyComplete();
        }

        @Test
        void create_sameCodeAndChannelType_conflicts() {
                TemplateEntity first = TemplateEntityFixtures.smsCaptcha();
                TemplateEntity second = TemplateEntityFixtures.smsCaptcha();

                StepVerifier.create(templateService.create(first)
                                .then(templateService.create(second)))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(ConflictException.class))
                                .verify();
        }

        @Test
        void create_sameCodeDifferentChannelType_conflicts() {
                TemplateEntity sms = TemplateEntityFixtures.smsCaptcha();
                TemplateEntity sameCodeEmail = TemplateEntityFixtures.smsCaptcha();
                sameCodeEmail.setChannelType(ChannelType.EMAIL);

                StepVerifier.create(templateService.create(sms)
                                .then(templateService.create(sameCodeEmail)))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(ConflictException.class))
                                .verify();
        }

        @Test
        void update_pushTemplate_replacesPayloadAndRefreshesUpdatedAt() {
                TemplateEntity input = TemplateEntityFixtures.pushNewMessage();

                StepVerifier.create(templateService.create(input)
                                .flatMap(saved -> {
                                        assertThat(saved.getId()).isNotBlank();
                                        long before = saved.getUpdatedAt();

                                        Map<Language, LocalizedTemplate> loc = new HashMap<>(
                                                        saved.getLocalizedTemplates());
                                        LocalizedTemplate zh = loc.get(Language.ZH_CN);
                                        loc.put(Language.ZH_CN,
                                                        new LocalizedTemplate(zh.getTitle(), "patched-zh-body"));

                                        TemplateEntity patch = new TemplateEntity();
                                        patch.setCode(saved.getCode());
                                        patch.setChannelType(saved.getChannelType());
                                        patch.setLocalizedTemplates(loc);

                                        return Mono.delay(Duration.ofMillis(2))
                                                        .then(templateService.update(saved.getId(), patch))
                                                        .flatMap(updated -> templateService
                                                                        .findById(updated.getId()))
                                                        .map(reloaded -> new UpdatedPushAssert(before, input,
                                                                        reloaded));
                                }))
                                .assertNext(u -> {
                                        assertThat(u.reloaded().getUpdatedAt())
                                                        .isGreaterThanOrEqualTo(u.beforeUpdatedAt());
                                        assertThat(u.reloaded().getLocalizedTemplates().get(Language.ZH_CN)
                                                        .getContent()).isEqualTo("patched-zh-body");
                                        LocalizedTemplate enIn = u.input().getLocalizedTemplates()
                                                        .get(Language.EN);
                                        LocalizedTemplate enOut = u.reloaded().getLocalizedTemplates()
                                                        .get(Language.EN);
                                        assertThat(enOut.getTitle()).isEqualTo(enIn.getTitle());
                                        assertThat(enOut.getContent()).isEqualTo(enIn.getContent());
                                })
                                .verifyComplete();
        }

        private record UpdatedPushAssert(long beforeUpdatedAt, TemplateEntity input,
                        TemplateEntity reloaded) {
        }

        @Test
        void update_welcomeEmail_toCaptchaSms_whenSmsExists_conflicts() {
                TemplateEntity sms = TemplateEntityFixtures.smsCaptcha();
                TemplateEntity welcome = TemplateEntityFixtures.welcomeEmail();

                StepVerifier.create(templateService.create(sms)
                                .then(templateService.create(welcome))
                                .then(templateService.findByCodeAndChannelType("welcome", ChannelType.EMAIL)
                                                .flatMap(w -> {
                                                        TemplateEntity patch = new TemplateEntity();
                                                        patch.setCode("captcha");
                                                        patch.setChannelType(ChannelType.SMS);
                                                        patch.setLocalizedTemplates(w.getLocalizedTemplates());
                                                        return templateService.update(w.getId(), patch);
                                                })))
                                .expectErrorSatisfies(ex -> assertThat(ex)
                                                .isInstanceOf(ConflictException.class))
                                .verify();
        }

        @Test
        void delete_emailTemplate_removesDocument() {
                TemplateEntity email = TemplateEntityFixtures.emailCaptcha();

                StepVerifier.create(templateService.create(email)
                                .flatMap(saved -> templateService.delete(saved.getId())
                                                // 直接通过 repository 验证数据库中的文档已被删除
                                                .then(templateRepository.findById(saved.getId()))))
                                .verifyComplete();
        }

        @Test
        void list_invalidLimitOrOffset_returnsEmpty() {
                StepVerifier.create(templateService.list(null, 10, -1).collectList())
                                .assertNext(list -> assertThat(list).isEmpty())
                                .verifyComplete();

                StepVerifier.create(templateService.list(null, 0, 0).collectList())
                                .assertNext(list -> assertThat(list).isEmpty())
                                .verifyComplete();
        }

        @Test
        void list_pagination_returnsExpectedCounts() {
                TemplateEntity a = TemplateEntityFixtures.smsCaptcha();
                TemplateEntity b = TemplateEntityFixtures.imOpsAlert();
                TemplateEntity c = TemplateEntityFixtures.pushNewMessage();

                StepVerifier.create(templateService.create(a)
                                .then(templateService.create(b))
                                .then(templateService.create(c))
                                .thenMany(Flux.zip(
                                                templateService.list(null, 2, 0).collectList(),
                                                templateService.list(null, 2, 2).collectList())))
                                .assertNext(tuple -> {
                                        assertThat(tuple.getT1()).hasSize(2);
                                        assertThat(tuple.getT2()).hasSize(1);
                                })
                                .verifyComplete();
        }

        private static void assertTemplateContentEqual(TemplateEntity expected, TemplateEntity actual) {
                assertThat(actual.getCode()).isEqualTo(expected.getCode());
                assertThat(actual.getChannelType()).isEqualTo(expected.getChannelType());
                Map<Language, LocalizedTemplate> exp = expected.getLocalizedTemplates();
                Map<Language, LocalizedTemplate> got = actual.getLocalizedTemplates();
                if (exp == null) {
                        assertThat(got).isNull();
                        return;
                }
                assertThat(got).isNotNull();
                assertThat(got).hasSameSizeAs(exp);
                exp.forEach((lang, tmpl) -> {
                        LocalizedTemplate a = got.get(lang);
                        assertThat(a).as("language %s", lang).isNotNull();
                        assertThat(a.getTitle()).isEqualTo(tmpl.getTitle());
                        assertThat(a.getContent()).isEqualTo(tmpl.getContent());
                });
        }
}