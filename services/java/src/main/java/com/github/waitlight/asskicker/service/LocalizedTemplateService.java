package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LocalizedTemplateEntity;
import com.github.waitlight.asskicker.repository.LocalizedTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalizedTemplateService {

    private final TemplateRepository templateRepository;
    private final LocalizedTemplateRepository localizedTemplateRepository;
    private final ProviderTemplateService providerTemplateService;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<LocalizedTemplateEntity>> localizedByTemplateIdAndLanguageCache;

    @PostConstruct
    void initCaches() {
        localizedByTemplateIdAndLanguageCache = caffeineCacheConfig.buildCache((key, executor) -> {
            int sep = key.indexOf('|');
            String templateId = key.substring(0, sep);
            Language language = Language.valueOf(key.substring(sep + 1));
            return localizedTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .toFuture();
        });
    }

    public Mono<LocalizedTemplateEntity> findLocalized(String templateId, Language language) {
        if (!StringUtils.hasText(templateId) || language == null) {
            return Mono.empty();
        }
        return Mono.fromFuture(localizedByTemplateIdAndLanguageCache.get(localizedCacheKey(templateId, language)))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    public Flux<LocalizedTemplateEntity> listLocalized(String templateId) {
        if (!StringUtils.hasText(templateId)) {
            return Flux.empty();
        }
        return localizedTemplateRepository.findByTemplateId(templateId);
    }

    public Mono<Void> deleteLocalized(String templateId, Language language) {
        if (!StringUtils.hasText(templateId) || language == null) {
            return Mono.error(new BadRequestException("template.localized.key.empty"));
        }
        return localizedTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound", templateId)))
                .flatMap(found -> providerTemplateService.deleteByLocalizedTemplateId(found.getId())
                        .then(localizedTemplateRepository.deleteById(found.getId()))
                        .doOnSuccess(v -> invalidateLocalizedCache(found)));
    }

    public Mono<LocalizedTemplateEntity> findLocalizedById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.localized.id.empty"));
        }
        return localizedTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound", id)));
    }

    public Mono<LocalizedTemplateEntity> createLocalized(LocalizedTemplateEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getTemplateId()) || entity.getLanguage() == null) {
            return Mono.error(new BadRequestException("template.localized.key.empty"));
        }
        return templateRepository.findById(entity.getTemplateId())
                .switchIfEmpty(Mono.error(new NotFoundException("template.id.notFound", entity.getTemplateId())))
                .then(Mono.defer(() -> localizedTemplateRepository
                        .findByTemplateIdAndLanguage(entity.getTemplateId(), entity.getLanguage())
                        .flatMap(existing -> Mono.<LocalizedTemplateEntity>error(
                                new ConflictException("template.localized.exists")))
                        .switchIfEmpty(Mono.defer(() -> localizedTemplateRepository.save(entity)
                                .doOnSuccess(this::invalidateLocalizedCache)))));
    }

    public Mono<LocalizedTemplateEntity> updateLocalized(String id, LocalizedTemplateEntity patch) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.localized.id.empty"));
        }
        return localizedTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound", id)))
                .flatMap(existing -> {
                    mergeLocalizedPatch(patch, existing);
                    return localizedTemplateRepository.save(existing)
                            .doOnSuccess(this::invalidateLocalizedCache);
                });
    }

    public Mono<Void> deleteLocalizedById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.localized.id.empty"));
        }
        return localizedTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound", id)))
                .flatMap(found -> providerTemplateService.deleteByLocalizedTemplateId(found.getId())
                        .then(localizedTemplateRepository.deleteById(found.getId()))
                        .doOnSuccess(v -> invalidateLocalizedCache(found)));
    }

    private void mergeLocalizedPatch(LocalizedTemplateEntity patch, LocalizedTemplateEntity target) {
        if (patch.getTitle() != null) {
            target.setTitle(patch.getTitle());
        }
        if (patch.getContent() != null) {
            target.setContent(patch.getContent());
        }
    }

    private void invalidateLocalizedCache(LocalizedTemplateEntity localized) {
        localizedByTemplateIdAndLanguageCache.synchronous()
                .invalidate(localizedCacheKey(localized.getTemplateId(), localized.getLanguage()));
    }

    private String localizedCacheKey(String templateId, Language language) {
        return templateId + "|" + language.name();
    }
}