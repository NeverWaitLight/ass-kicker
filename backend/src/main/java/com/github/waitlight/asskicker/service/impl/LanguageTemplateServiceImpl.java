package com.github.waitlight.asskicker.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.service.LanguageTemplateService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
public class LanguageTemplateServiceImpl implements LanguageTemplateService {

    private final LanguageTemplateRepository languageTemplateRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<LanguageTemplate>> ltByTemplateIdAndLanguageCache;

    public LanguageTemplateServiceImpl(LanguageTemplateRepository languageTemplateRepository,
                                       CaffeineCacheConfig caffeineCacheConfig) {
        this.languageTemplateRepository = languageTemplateRepository;
        this.caffeineCacheConfig = caffeineCacheConfig;
    }

    @PostConstruct
    void initCaches() {
        ltByTemplateIdAndLanguageCache = caffeineCacheConfig.buildCache((key, executor) -> {
            String[] parts = key.split(":", 2);
            Language language = Language.valueOf(parts[1]);
            return languageTemplateRepository.findByTemplateIdAndLanguage(parts[0], language)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .toFuture();
        });
    }

    @Override
    public Flux<LanguageTemplate> findAllByTemplateId(String templateId) {
        return languageTemplateRepository.findByTemplateId(templateId);
    }

    @Override
    public Mono<LanguageTemplate> findByTemplateIdAndLanguage(String templateId, Language language) {
        String key = templateId + ":" + language.name();
        return Mono.fromFuture(ltByTemplateIdAndLanguageCache.get(key))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Mono<LanguageTemplate> save(LanguageTemplate languageTemplate) {
        languageTemplate.setId(null);
        long timestamp = Instant.now().toEpochMilli();
        languageTemplate.setCreatedAt(timestamp);
        languageTemplate.setUpdatedAt(timestamp);
        return languageTemplateRepository.save(languageTemplate);
    }

    @Override
    public Mono<LanguageTemplate> update(String id, LanguageTemplate languageTemplate) {
        return languageTemplateRepository.findById(id)
                .flatMap(existingLT -> {
                    Language oldLanguage = existingLT.getLanguage();
                    String templateId = existingLT.getTemplateId();
                    existingLT.setLanguage(languageTemplate.getLanguage());
                    existingLT.setContent(languageTemplate.getContent());
                    existingLT.setUpdatedAt(Instant.now().toEpochMilli());
                    return languageTemplateRepository.save(existingLT)
                            .doOnSuccess(saved -> {
                                ltByTemplateIdAndLanguageCache.synchronous()
                                        .invalidate(templateId + ":" + oldLanguage.name());
                                ltByTemplateIdAndLanguageCache.synchronous()
                                        .invalidate(templateId + ":" + saved.getLanguage().name());
                            });
                });
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return languageTemplateRepository.findById(id)
                .flatMap(lt -> languageTemplateRepository.deleteById(id)
                        .doOnSuccess(v -> ltByTemplateIdAndLanguageCache.synchronous()
                                .invalidate(lt.getTemplateId() + ":" + lt.getLanguage().name())));
    }
}
