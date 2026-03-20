package com.github.waitlight.asskicker.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import com.github.waitlight.asskicker.service.TemplateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final LanguageTemplateRepository languageTemplateRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByIdCache;
    private AsyncLoadingCache<String, Optional<TemplateEntity>> templateByCodeCache;
    private AsyncLoadingCache<String, Optional<LanguageTemplateEntity>> languageTemplateCache;

    @PostConstruct
    void initCaches() {
        templateByIdCache = caffeineCacheConfig.buildCache((id, executor) -> templateRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        templateByCodeCache = caffeineCacheConfig.buildCache((code, executor) -> templateRepository.findByCode(code)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        languageTemplateCache = caffeineCacheConfig.buildCache((key, executor) -> {
            String[] parts = key.split(":", 2);
            Language language = Language.valueOf(parts[1]);
            return languageTemplateRepository.findByTemplateIdAndLanguage(parts[0], language)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .toFuture();
        });
    }

    @Override
    public Flux<TemplateEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return templateRepository.findAll()
                .skip(offset)
                .take(size);
    }

    @Override
    public Mono<TemplateEntity> findById(String id) {
        return Mono.fromFuture(templateByIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Mono<TemplateEntity> findByCode(String code) {
        return Mono.fromFuture(templateByCodeCache.get(code))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Mono<TemplateEntity> createTemplate(TemplateEntity template) {
        template.setId(null);
        long timestamp = Instant.now().toEpochMilli();
        template.setCreatedAt(timestamp);
        template.setUpdatedAt(timestamp);
        return templateRepository.save(template);
    }

    @Override
    public Mono<TemplateEntity> updateTemplate(String id, TemplateEntity template) {
        return templateRepository.findById(id)
                .flatMap(existingTemplate -> {
                    String oldCode = existingTemplate.getCode();
                    existingTemplate.setName(template.getName());
                    existingTemplate.setCode(template.getCode());
                    existingTemplate.setDescription(template.getDescription());
                    existingTemplate.setChannelType(template.getChannelType());
                    existingTemplate.setAttributes(template.getAttributes());
                    existingTemplate.setUpdatedAt(Instant.now().toEpochMilli());
                    return templateRepository.save(existingTemplate)
                            .doOnSuccess(saved -> {
                                templateByIdCache.synchronous().invalidate(id);
                                templateByCodeCache.synchronous().invalidate(oldCode);
                                templateByCodeCache.synchronous().invalidate(saved.getCode());
                            });
                });
    }

    @Override
    public Mono<Void> deleteTemplate(String id) {
        return templateRepository.findById(id)
                .flatMap(template -> templateRepository.deleteById(id)
                        .doOnSuccess(v -> {
                            templateByIdCache.synchronous().invalidate(id);
                            templateByCodeCache.synchronous().invalidate(template.getCode());
                        }));
    }

    @Override
    public Mono<TemplateEntity> getTemplateById(String id) {
        return findById(id);
    }

    @Override
    public Mono<LanguageTemplateEntity> getTemplateContentByLanguage(String templateId, Language language) {
        String key = templateId + ":" + language.name();
        return Mono.fromFuture(languageTemplateCache.get(key))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    @Override
    public Mono<LanguageTemplateEntity> saveTemplateContentByLanguage(String templateId, Language language, String content) {
        return templateRepository.findById(templateId)
                .flatMap(ignored -> languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                        .flatMap(existingLT -> {
                            existingLT.setContent(content);
                            existingLT.setUpdatedAt(Instant.now().toEpochMilli());
                            return languageTemplateRepository.save(existingLT);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            long timestamp = Instant.now().toEpochMilli();
                            LanguageTemplateEntity created = new LanguageTemplateEntity(templateId, language, content);
                            created.setCreatedAt(timestamp);
                            created.setUpdatedAt(timestamp);
                            return languageTemplateRepository.save(created);
                        })))
                .doOnSuccess(
                        saved -> languageTemplateCache.synchronous().invalidate(templateId + ":" + language.name()));
    }

    @Override
    public Flux<LanguageTemplateEntity> getAllTemplateContentsByTemplateId(String templateId) {
        return languageTemplateRepository.findByTemplateId(templateId);
    }

    @Override
    public Mono<Void> deleteTemplateContentByLanguage(String templateId, Language language) {
        return languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                .flatMap(languageTemplateRepository::delete)
                .doOnSuccess(v -> languageTemplateCache.synchronous().invalidate(templateId + ":" + language.name()));
    }
}
