package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.github.waitlight.asskicker.config.CaffeineCacheProperties;
import com.github.waitlight.asskicker.dto.UniSendMessageReq;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.service.MessageTemplateEntityFixtures;
import com.github.waitlight.asskicker.service.MessageTemplateService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MessageTemplateEngineTest {

    @Mock
    private MessageTemplateService messageTemplateService;

    private MessageTemplateEngine engine;

    @BeforeEach
    void setUp() {
        CaffeineCacheProperties cacheProperties = new CaffeineCacheProperties();
        cacheProperties.setMaximumSize(100);
        cacheProperties.setExpireAfterWriteMinutes(60);
        engine = new MessageTemplateEngine(messageTemplateService, cacheProperties);
    }

    @Test
    void fill_rendersMustache_andCopiesTitleAndExtraData() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.smsCaptchaZhCn();
        when(messageTemplateService.findByCode("sms_captcha")).thenReturn(Mono.just(entity));

        UniSendMessageReq req = new UniSendMessageReq();
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
                    assertThat(msg.getTitle()).isEqualTo("验证码");
                    assertThat(msg.getContent()).isEqualTo("您好 王德发，您的验证码是 123456");
                    assertThat(msg.getExtraData()).isEqualTo(extra);
                })
                .verifyComplete();

        verify(messageTemplateService).findByCode("sms_captcha");
    }

    @Test
    void fill_whenTemplateNotFound_completesEmpty() {
        when(messageTemplateService.findByCode("missing")).thenReturn(Mono.empty());

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("missing");
        req.setLanguage(Language.ZH_CN);

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedMissing_completesEmpty() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.localizedEmpty();
        when(messageTemplateService.findByCode("x")).thenReturn(Mono.just(entity));

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("x");
        req.setLanguage(Language.EN);

        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenLocalizedTemplatesNull_completesEmpty() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.localizedTemplatesNull();
        when(messageTemplateService.findByCode("x")).thenReturn(Mono.just(entity));
        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("x");
        req.setLanguage(Language.ZH_CN);
        StepVerifier.create(engine.fill(req)).verifyComplete();
    }

    @Test
    void fill_whenTemplateParamsNull_rendersWithoutSubstitution() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.greetEn();
        when(messageTemplateService.findByCode("greet")).thenReturn(Mono.just(entity));

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("greet");
        req.setLanguage(Language.EN);
        req.setTemplateParams(null);

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> assertThat(msg.getContent()).isEqualTo("Hello "))
                .verifyComplete();
    }

    @Test
    void fill_whenTemplateContentNull_rendersEmptyString() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.emptyBodyDe();
        when(messageTemplateService.findByCode("empty_body")).thenReturn(Mono.just(entity));

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("empty_body");
        req.setLanguage(Language.DE);

        StepVerifier.create(engine.fill(req))
                .assertNext(msg -> assertThat(msg.getContent()).isEmpty())
                .verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidateCompiledTemplates_removesCacheEntriesForPrefix() {
        MessageTemplateEntity entity = MessageTemplateEntityFixtures.invZhCn();
        when(messageTemplateService.findByCode("inv")).thenReturn(Mono.just(entity));

        UniSendMessageReq req = new UniSendMessageReq();
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
