package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.config.CaffeineCacheProperties;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 /v1/send 流程中 TemplateServiceImpl 缓存是否生效
 *
 * send 流程：TemplateManager.fill() → findByCode() +
 * getTemplateContentByLanguage()
 * 两个方法都由 Caffeine AsyncLoadingCache 驱动，重复调用应只访问一次 Repository
 */
class TemplateServiceImplCacheTest {

    private TemplateRepository templateRepository;
    private LanguageTemplateRepository languageTemplateRepository;
    private TemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateRepository = mock(TemplateRepository.class);
        languageTemplateRepository = mock(LanguageTemplateRepository.class);

        CaffeineCacheProperties properties = new CaffeineCacheProperties();
        properties.setMaximumSize(1000);
        properties.setExpireAfterWriteMinutes(10);
        properties.setRandomJitterPercent(0);

        CaffeineCacheConfig cacheConfig = new CaffeineCacheConfig(properties);
        templateService = new TemplateServiceImpl(templateRepository, languageTemplateRepository, cacheConfig);
        templateService.initCaches();
    }

    @Test
    void findByCode_secondCallShouldHitCache() {
        Template template = new Template("Test", "tpl-code", "desc");
        template.setId("id-1");
        when(templateRepository.findByCode("tpl-code")).thenReturn(Mono.just(template));

        StepVerifier.create(templateService.findByCode("tpl-code"))
                .expectNext(template)
                .verifyComplete();

        StepVerifier.create(templateService.findByCode("tpl-code"))
                .expectNext(template)
                .verifyComplete();

        verify(templateRepository, times(1)).findByCode("tpl-code");
    }

    @Test
    void findByCode_emptyResult_shouldAlsoBeCached() {
        when(templateRepository.findByCode("missing")).thenReturn(Mono.empty());

        StepVerifier.create(templateService.findByCode("missing")).verifyComplete();
        StepVerifier.create(templateService.findByCode("missing")).verifyComplete();

        verify(templateRepository, times(1)).findByCode("missing");
    }

    @Test
    void getTemplateContentByLanguage_secondCallShouldHitCache() {
        LanguageTemplate lt = new LanguageTemplate("id-1", Language.EN, "Hello {{name}}");
        lt.setId("lt-1");
        when(languageTemplateRepository.findByTemplateIdAndLanguage("id-1", Language.EN))
                .thenReturn(Mono.just(lt));

        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(lt)
                .verifyComplete();

        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(lt)
                .verifyComplete();

        verify(languageTemplateRepository, times(1)).findByTemplateIdAndLanguage("id-1", Language.EN);
    }

    @Test
    void getTemplateContentByLanguage_differentLanguageKeys_cachedIndependently() {
        LanguageTemplate ltEn = new LanguageTemplate("id-1", Language.EN, "Hello {{name}}");
        LanguageTemplate ltZh = new LanguageTemplate("id-1", Language.ZH_HANS, "你好 {{name}}");
        when(languageTemplateRepository.findByTemplateIdAndLanguage("id-1", Language.EN))
                .thenReturn(Mono.just(ltEn));
        when(languageTemplateRepository.findByTemplateIdAndLanguage("id-1", Language.ZH_HANS))
                .thenReturn(Mono.just(ltZh));

        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(ltEn).verifyComplete();
        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.ZH_HANS))
                .expectNext(ltZh).verifyComplete();

        // 再次调用，均命中缓存
        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(ltEn).verifyComplete();
        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.ZH_HANS))
                .expectNext(ltZh).verifyComplete();

        verify(languageTemplateRepository, times(1)).findByTemplateIdAndLanguage("id-1", Language.EN);
        verify(languageTemplateRepository, times(1)).findByTemplateIdAndLanguage("id-1", Language.ZH_HANS);
    }

    @Test
    void updateTemplate_shouldInvalidateFindByCodeCache() {
        Template original = new Template("Old", "tpl-code", "desc");
        original.setId("id-1");
        when(templateRepository.findByCode("tpl-code")).thenReturn(Mono.just(original));

        // 预热缓存
        StepVerifier.create(templateService.findByCode("tpl-code"))
                .expectNext(original)
                .verifyComplete();
        verify(templateRepository, times(1)).findByCode("tpl-code");

        // 执行更新，触发缓存失效
        Template payload = new Template("New", "tpl-code", "desc");
        when(templateRepository.findById("id-1")).thenReturn(Mono.just(original));
        when(templateRepository.save(any())).thenReturn(Mono.just(original));
        StepVerifier.create(templateService.updateTemplate("id-1", payload))
                .expectNextCount(1)
                .verifyComplete();

        // 缓存失效后再次查询，Repository 应被重新调用
        StepVerifier.create(templateService.findByCode("tpl-code"))
                .expectNext(original)
                .verifyComplete();
        verify(templateRepository, times(2)).findByCode("tpl-code");
    }

    @Test
    void deleteTemplate_shouldInvalidateFindByCodeAndFindByIdCaches() {
        Template template = new Template("Test", "tpl-code", "desc");
        template.setId("id-1");
        when(templateRepository.findByCode("tpl-code")).thenReturn(Mono.just(template));
        when(templateRepository.findById("id-1")).thenReturn(Mono.just(template));
        when(templateRepository.deleteById("id-1")).thenReturn(Mono.empty());

        // 预热两个缓存
        StepVerifier.create(templateService.findByCode("tpl-code"))
                .expectNext(template).verifyComplete();
        StepVerifier.create(templateService.findById("id-1"))
                .expectNext(template).verifyComplete();

        verify(templateRepository, times(1)).findByCode("tpl-code");
        verify(templateRepository, times(1)).findById("id-1");

        // 删除触发缓存失效
        StepVerifier.create(templateService.deleteTemplate("id-1")).verifyComplete();

        // 再次查询，缓存已失效，需重新访问 Repository
        when(templateRepository.findByCode("tpl-code")).thenReturn(Mono.empty());
        when(templateRepository.findById("id-1")).thenReturn(Mono.empty());
        StepVerifier.create(templateService.findByCode("tpl-code")).verifyComplete();
        StepVerifier.create(templateService.findById("id-1")).verifyComplete();

        // findByCode: 预热 1 次 + 失效后重查 1 次 = 2
        // findById:  预热 1 次（缓存 loader）+ deleteTemplate 内部直接调用 1 次 + 失效后重查 1 次 = 3
        verify(templateRepository, times(2)).findByCode("tpl-code");
        verify(templateRepository, times(3)).findById("id-1");
    }

    @Test
    void saveTemplateContentByLanguage_shouldInvalidateLanguageTemplateCache() {
        Template template = new Template("Test", "tpl-code", "desc");
        template.setId("id-1");
        LanguageTemplate lt = new LanguageTemplate("id-1", Language.EN, "Hello {{name}}");
        lt.setId("lt-1");

        when(templateRepository.findById("id-1")).thenReturn(Mono.just(template));
        when(languageTemplateRepository.findByTemplateIdAndLanguage("id-1", Language.EN))
                .thenReturn(Mono.just(lt));
        when(languageTemplateRepository.save(any())).thenReturn(Mono.just(lt));

        // 预热缓存
        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(lt).verifyComplete();
        verify(languageTemplateRepository, times(1)).findByTemplateIdAndLanguage("id-1", Language.EN);

        // 保存新内容，触发缓存失效
        StepVerifier.create(templateService.saveTemplateContentByLanguage("id-1", Language.EN, "Hi {{name}}"))
                .expectNextCount(1)
                .verifyComplete();

        // 缓存失效后，下次查询重新访问 Repository
        StepVerifier.create(templateService.getTemplateContentByLanguage("id-1", Language.EN))
                .expectNext(lt).verifyComplete();

        // 1 次预热 + 1 次 saveTemplateContentByLanguage 内部直接调 repo + 1 次失效后查询 = 3
        verify(languageTemplateRepository, times(3)).findByTemplateIdAndLanguage("id-1", Language.EN);
    }
}
