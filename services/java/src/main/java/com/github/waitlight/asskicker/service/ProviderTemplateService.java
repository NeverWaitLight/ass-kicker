package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ProviderTemplateEntity;
import com.github.waitlight.asskicker.repository.LocalizedTemplateRepository;
import com.github.waitlight.asskicker.repository.ProviderTemplateRepository;
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
public class ProviderTemplateService {

    private final LocalizedTemplateRepository localizedTemplateRepository;
    private final ProviderTemplateRepository providerTemplateRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<ProviderTemplateEntity>> providerByLocalizedIdAndProviderCache;

    @PostConstruct
    void initCaches() {
        providerByLocalizedIdAndProviderCache = caffeineCacheConfig.buildCache((key, executor) -> {
            int sep = key.indexOf('|');
            String localizedTemplateId = key.substring(0, sep);
            ChannelProvider provider = ChannelProvider.valueOf(key.substring(sep + 1));
            return providerTemplateRepository
                    .findByLocalizedTemplateIdAndProvider(localizedTemplateId, provider)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .toFuture();
        });
    }

    public Mono<ProviderTemplateEntity> findProvider(String localizedTemplateId, ChannelProvider provider) {
        if (!StringUtils.hasText(localizedTemplateId) || provider == null) {
            return Mono.empty();
        }
        return Mono.fromFuture(providerByLocalizedIdAndProviderCache
                        .get(providerCacheKey(localizedTemplateId, provider)))
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty));
    }

    public Flux<ProviderTemplateEntity> listProvider(String localizedTemplateId) {
        if (!StringUtils.hasText(localizedTemplateId)) {
            return Flux.empty();
        }
        return providerTemplateRepository.findByLocalizedTemplateId(localizedTemplateId);
    }

    public Mono<ProviderTemplateEntity> findProviderById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.provider.id.empty"));
        }
        return providerTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.provider.notFound", id)));
    }

    public Mono<ProviderTemplateEntity> createProvider(ProviderTemplateEntity entity) {
        if (entity == null || !StringUtils.hasText(entity.getLocalizedTemplateId()) || entity.getProvider() == null) {
            return Mono.error(new BadRequestException("template.provider.key.empty"));
        }
        return localizedTemplateRepository.findById(entity.getLocalizedTemplateId())
                .switchIfEmpty(Mono.error(new NotFoundException("template.localized.notFound",
                        entity.getLocalizedTemplateId())))
                .then(Mono.defer(() -> providerTemplateRepository
                        .findByLocalizedTemplateIdAndProvider(entity.getLocalizedTemplateId(), entity.getProvider())
                        .flatMap(existing -> Mono.<ProviderTemplateEntity>error(
                                new ConflictException("template.provider.exists")))
                        .switchIfEmpty(Mono.defer(() -> providerTemplateRepository.save(entity)
                                .doOnSuccess(this::invalidateProviderCache)))));
    }

    public Mono<ProviderTemplateEntity> updateProvider(String id, ProviderTemplateEntity patch) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.provider.id.empty"));
        }
        return providerTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.provider.notFound", id)))
                .flatMap(existing -> {
                    mergeProviderPatch(patch, existing);
                    return providerTemplateRepository.save(existing)
                            .doOnSuccess(this::invalidateProviderCache);
                });
    }

    public Mono<Void> deleteProviderById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("template.provider.id.empty"));
        }
        return providerTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("template.provider.notFound", id)))
                .flatMap(found -> providerTemplateRepository.deleteById(found.getId())
                        .doOnSuccess(v -> invalidateProviderCache(found)));
    }

    public Mono<Void> deleteProvider(String localizedTemplateId, ChannelProvider provider) {
        if (!StringUtils.hasText(localizedTemplateId) || provider == null) {
            return Mono.error(new BadRequestException("template.provider.key.empty"));
        }
        return providerTemplateRepository.findByLocalizedTemplateIdAndProvider(localizedTemplateId, provider)
                .switchIfEmpty(Mono.error(new NotFoundException("template.provider.notFound", localizedTemplateId)))
                .flatMap(found -> providerTemplateRepository.deleteById(found.getId())
                        .doOnSuccess(v -> invalidateProviderCache(found)));
    }

    public Mono<Void> deleteByLocalizedTemplateId(String localizedTemplateId) {
        if (!StringUtils.hasText(localizedTemplateId)) {
            return Mono.empty();
        }
        return providerTemplateRepository.findByLocalizedTemplateId(localizedTemplateId)
                .doOnNext(this::invalidateProviderCache)
                .then(providerTemplateRepository.deleteByLocalizedTemplateId(localizedTemplateId));
    }

    private void mergeProviderPatch(ProviderTemplateEntity patch, ProviderTemplateEntity target) {
        if (patch.getProviderTemplateCode() != null) {
            target.setProviderTemplateCode(patch.getProviderTemplateCode());
        }
        if (patch.getUploadedAt() != null) {
            target.setUploadedAt(patch.getUploadedAt());
        }
        if (patch.getFailureReason() != null) {
            target.setFailureReason(patch.getFailureReason());
        }
    }

    private void invalidateProviderCache(ProviderTemplateEntity provider) {
        providerByLocalizedIdAndProviderCache.synchronous()
                .invalidate(providerCacheKey(provider.getLocalizedTemplateId(), provider.getProvider()));
    }

    private String providerCacheKey(String localizedTemplateId, ChannelProvider provider) {
        return localizedTemplateId + "|" + provider.name();
    }
}
