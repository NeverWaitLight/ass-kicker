package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.mustachejava.Mustache;
import com.github.waitlight.asskicker.channel.impl.EmailReq;
import com.github.waitlight.asskicker.config.CaffeineCacheProperties;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.service.TemplateEngine;
import com.github.waitlight.asskicker.service.TemplateEntityFixtures;
import com.github.waitlight.asskicker.service.TemplateService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TemplateEngineTest {

    @Mock
    private TemplateService templateService;

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        CaffeineCacheProperties cacheProperties = new CaffeineCacheProperties();
        cacheProperties.setMaximumSize(100);
        cacheProperties.setExpireAfterWriteMinutes(60);
        engine = new TemplateEngine(templateService, cacheProperties);
    }

    private static EmailReq emailReq(String templateCode, Language language, Map<String, String> params) {
        EmailReq req = new EmailReq();
        req.setType(ChannelType.EMAIL);
        req.setTo(List.of("dev@example.com"));
        req.setSubject("placeholder");
        req.setBody("placeholder");
        req.setTemplateCode(templateCode);
        req.setLanguage(language);
        req.setTemplateParams(params);
        return req;
    }

    @Test
    void fill_rendersMustache_intoSubjectAndBody() {
        TemplateEntity entity = TemplateEntityFixtures.smsCaptchaZhCn();
        when(templateService.findByCode("sms_captcha")).thenReturn(Mono.just(entity));

        Map<String, String> params = new HashMap<>();
        params.put("name", "王德发");
        params.put("code", "123456");
        EmailReq req = emailReq("sms_captcha", Language.ZH_CN, params);

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> {
                    assertThat(filled.getSubject()).isEqualTo("验证码");
                    assertThat(filled.getBody()).isEqualTo("您好 王德发，您的验证码是 123456");
                })
                .verifyComplete();

        verify(templateService).findByCode("sms_captcha");
    }

    @Test
    void fill_whenDirectSend_skipsTemplateLookup() {
        EmailReq req = emailReq("sms_captcha", Language.ZH_CN, Map.of());
        req.setDirectSend(true);

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> {
                    assertThat(filled.getSubject()).isEqualTo("placeholder");
                    assertThat(filled.getBody()).isEqualTo("placeholder");
                })
                .verifyComplete();

        verifyNoInteractions(templateService);
    }

    @Test
    void fill_whenTemplateCodeBlank_skipsTemplateLookup() {
        EmailReq req = emailReq("", Language.ZH_CN, Map.of());

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> {
                    assertThat(filled.getSubject()).isEqualTo("placeholder");
                    assertThat(filled.getBody()).isEqualTo("placeholder");
                })
                .verifyComplete();

        verifyNoInteractions(templateService);
    }

    @Test
    void fill_whenTemplateNotFound_completesEmpty() {
        when(templateService.findByCode("missing")).thenReturn(Mono.empty());

        EmailReq req = emailReq("missing", Language.ZH_CN, Map.of());

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedMissing_completesEmpty() {
        TemplateEntity entity = TemplateEntityFixtures.localizedEmpty();
        when(templateService.findByCode("x")).thenReturn(Mono.just(entity));

        EmailReq req = emailReq("x", Language.EN, Map.of());

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedTemplatesNull_completesEmpty() {
        TemplateEntity entity = TemplateEntityFixtures.localizedTemplatesNull();
        when(templateService.findByCode("x")).thenReturn(Mono.just(entity));

        EmailReq req = emailReq("x", Language.ZH_CN, Map.of());

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenTemplateParamsNull_rendersWithoutSubstitution() {
        TemplateEntity entity = TemplateEntityFixtures.greetEn();
        when(templateService.findByCode("greet")).thenReturn(Mono.just(entity));

        EmailReq req = emailReq("greet", Language.EN, null);

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> {
                    assertThat(filled.getSubject()).isEqualTo("t");
                    assertThat(filled.getBody()).isEqualTo("Hello ");
                })
                .verifyComplete();
    }

    @Test
    void fill_whenTemplateContentNull_rendersEmptyStringWithoutOverwritingBody() {
        TemplateEntity entity = TemplateEntityFixtures.emptyBodyDe();
        when(templateService.findByCode("empty_body")).thenReturn(Mono.just(entity));

        EmailReq req = emailReq("empty_body", Language.DE, Map.of());

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> {
                    assertThat(filled.getSubject()).isEqualTo("only title");
                    assertThat(filled.getBody()).isEqualTo("placeholder");
                })
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidateCompiledTemplates_removesCacheEntriesForPrefix() {
        TemplateEntity entity = TemplateEntityFixtures.invZhCn();
        when(templateService.findByCode("inv")).thenReturn(Mono.just(entity));

        EmailReq req = emailReq("inv", Language.ZH_CN, Map.of("p", "1"));

        StepVerifier.create(engine.fill(req))
                .assertNext(filled -> assertThat(filled.getBody()).isEqualTo("x 1"))
                .verifyComplete();

        Cache<String, Mustache> cache = (Cache<String, Mustache>) ReflectionTestUtils.getField(engine,
                "compiledTemplateCache");

        assertThat(cache.asMap().keySet().stream().anyMatch(k -> k.startsWith("tpl-inv-zh_cn@"))).isTrue();

        engine.invalidateCompiledTemplates("inv", Language.ZH_CN);

        assertThat(cache.asMap().keySet().stream().noneMatch(k -> k.startsWith("tpl-inv-zh_cn@"))).isTrue();
    }
}
