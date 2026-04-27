package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.mustachejava.Mustache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheProperties;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.service.GlobalVariableService;
import com.github.waitlight.asskicker.service.TemplateEntityFixtures;
import com.github.waitlight.asskicker.service.TemplateService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TemplateEngineTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private GlobalVariableService globalVariableService;

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        CaffeineCacheProperties cacheProperties = new CaffeineCacheProperties();
        cacheProperties.setMaximumSize(100);
        cacheProperties.setExpireAfterWriteMinutes(60);
        engine = new TemplateEngine(templateService, globalVariableService, cacheProperties);
        lenient().when(globalVariableService.findEnabledVariablesMap()).thenReturn(Mono.just(Map.of()));
    }

    @Test
    void fill_rendersMustache_andCopiesTitleAndExtraData() {
        TemplateEntity entity = TemplateEntityFixtures.smsCaptchaZhCn();
        when(templateService.findByCode("sms_captcha")).thenReturn(Mono.just(entity));

        UniMessage req = new UniMessage();
        req.setTemplateCode("sms_captcha");
        req.setLanguage(Language.ZH_CN);
        Map<String, Object> params = new HashMap<>();
        params.put("name", "王德发");
        params.put("code", "123456");
        req.setTemplateParams(params);
        Map<String, Object> extra = new HashMap<>();
        extra.put("k", "v");
        req.setExtraData(extra);
        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> {
                    assertThat(msg.getTemplateCode()).isEqualTo("sms_captcha");
                    assertThat(msg.getLanguage()).isEqualTo(Language.ZH_CN);
                    assertThat(msg.getTitle()).isEqualTo("验证码");
                    assertThat(msg.getContent()).isEqualTo("您好 王德发，您的验证码是 123456");
                    assertThat(msg.getExtraData()).isEqualTo(extra);
                    assertThat(msg.getTemplateParams()).containsEntry("name", "王德发")
                            .containsEntry("code", "123456");
                })
                .verifyComplete();

        verify(templateService).findByCode("sms_captcha");
    }

    @Test
    void fill_mergesGlobalVariables_rendersTitleAndContent() {
        TemplateEntity entity = TemplateEntityFixtures.brandWelcomeEmail();
        when(templateService.findByCode("brand_welcome")).thenReturn(Mono.just(entity));
        when(globalVariableService.findEnabledVariablesMap()).thenReturn(Mono.just(Map.of(
                "brandName", "Ass Kicker",
                "teamName", "Ops Team")));

        UniMessage req = new UniMessage();
        req.setTemplateCode("brand_welcome");
        req.setLanguage(Language.EN);
        req.setTemplateParams(Map.of("name", "Alice"));

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> {
                    assertThat(msg.getTitle()).isEqualTo("Welcome to Ass Kicker from Ops Team");
                    assertThat(msg.getContent()).isEqualTo("Hello Alice, Ass Kicker is ready");
                    assertThat(msg.getTemplateParams())
                            .containsEntry("brandName", "Ass Kicker")
                            .containsEntry("teamName", "Ops Team")
                            .containsEntry("name", "Alice");
                })
                .verifyComplete();
    }

    @Test
    void fill_requestParamsOverrideGlobalVariables() {
        TemplateEntity entity = TemplateEntityFixtures.brandWelcomeEmail();
        when(templateService.findByCode("brand_welcome")).thenReturn(Mono.just(entity));
        when(globalVariableService.findEnabledVariablesMap()).thenReturn(Mono.just(Map.of(
                "brandName", "Global Brand",
                "teamName", "Global Team")));

        UniMessage req = new UniMessage();
        req.setTemplateCode("brand_welcome");
        req.setLanguage(Language.EN);
        req.setTemplateParams(Map.of(
                "brandName", "Request Brand",
                "name", "Bob"));

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> {
                    assertThat(msg.getTitle()).isEqualTo("Welcome to Request Brand from Global Team");
                    assertThat(msg.getContent()).isEqualTo("Hello Bob, Request Brand is ready");
                    assertThat(msg.getTemplateParams()).containsEntry("brandName", "Request Brand");
                })
                .verifyComplete();
    }

    @Test
    void fill_whenTemplateNotFound_completesEmpty() {
        when(templateService.findByCode("missing")).thenReturn(Mono.empty());

        UniMessage req = new UniMessage();
        req.setTemplateCode("missing");
        req.setLanguage(Language.ZH_CN);

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedMissing_completesEmpty() {
        TemplateEntity entity = TemplateEntityFixtures.localizedEmpty();
        when(templateService.findByCode("x")).thenReturn(Mono.just(entity));

        UniMessage req = new UniMessage();
        req.setTemplateCode("x");
        req.setLanguage(Language.EN);

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedTemplatesNull_completesEmpty() {
        TemplateEntity entity = TemplateEntityFixtures.localizedTemplatesNull();
        when(templateService.findByCode("x")).thenReturn(Mono.just(entity));
        UniMessage req = new UniMessage();
        req.setTemplateCode("x");
        req.setLanguage(Language.ZH_CN);
        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenTemplateParamsNull_rendersWithoutSubstitution() {
        TemplateEntity entity = TemplateEntityFixtures.greetEn();
        when(templateService.findByCode("greet")).thenReturn(Mono.just(entity));

        UniMessage req = new UniMessage();
        req.setTemplateCode("greet");
        req.setLanguage(Language.EN);
        req.setTemplateParams(null);

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> {
                    assertThat(msg.getContent()).isEqualTo("Hello ");
                    assertThat(msg.getTemplateParams()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void fill_whenTemplateContentNull_rendersEmptyString() {
        TemplateEntity entity = TemplateEntityFixtures.emptyBodyDe();
        when(templateService.findByCode("empty_body")).thenReturn(Mono.just(entity));

        UniMessage req = new UniMessage();
        req.setTemplateCode("empty_body");
        req.setLanguage(Language.DE);

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> assertThat(msg.getContent()).isEmpty())
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidateCompiledTemplates_removesCacheEntriesForPrefix() {
        TemplateEntity entity = TemplateEntityFixtures.invZhCn();
        when(templateService.findByCode("inv")).thenReturn(Mono.just(entity));

        UniMessage req = new UniMessage();
        req.setTemplateCode("inv");
        req.setLanguage(Language.ZH_CN);
        req.setTemplateParams(Map.of("p", "1"));

        StepVerifier.create(engine.fill(req))
                .assertNext(m -> assertThat(m.getContent()).isEqualTo("x 1"))
                .verifyComplete();

        Cache<String, Mustache> cache = (Cache<String, Mustache>) ReflectionTestUtils.getField(engine,
                "compiledTemplateCache");

        assertThat(cache.asMap().keySet().stream().anyMatch(k -> k.startsWith("tpl-inv-zh_cn@"))).isTrue();

        engine.invalidateCompiledTemplates("inv", Language.ZH_CN);

        assertThat(cache.asMap().keySet().stream().noneMatch(k -> k.startsWith("tpl-inv-zh_cn@"))).isTrue();
    }
}
