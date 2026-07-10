package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelTemplateEntity;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.ChannelTemplateRepository;
import com.github.waitlight.asskicker.repository.LocalizedTemplateRepository;
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
public class ChannelTemplateService {

    private final ChannelTemplateRepository channelTemplateRepository;
    private final LocalizedTemplateRepository localizedTemplateRepository;
    private final ChannelRepository channelRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<ChannelTemplateEntity>> byIdCache;
    private AsyncLoadingCache<String, Optional<ChannelTemplateEntity>> byLocalizedAndChannelCache;

    @PostConstruct
    void initCaches() {
        byIdCache = caffeineCacheConfig.buildCache((id, executor) -> channelTemplateRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        byLocalizedAndChannelCache = caffeineCacheConfig.buildCache((key, executor) -> {
            int sep = key.indexOf('|');
            String localizedTemplateId = key.substring(0, sep);
            String channelId = key.substring(sep + 1);
            return channelTemplateRepository
                    .findByLocalizedTemplateIdAndChannelId(localizedTemplateId, channelId)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .toFuture();
        });
    }

    public Mono<ChannelTemplateEntity> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("channelTemplate.id.empty"));
        }
        return Mono.fromFuture(byIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<ChannelTemplateEntity> getById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("channelTemplate.id.empty"));
        }
        return Mono.fromFuture(byIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("channelTemplate.notFound", id))));
    }

    public Mono<ChannelTemplateEntity> findByLocalizedTemplateIdAndChannelId(String localizedTemplateId,
            String channelId) {
        if (!StringUtils.hasText(localizedTemplateId) || !StringUtils.hasText(channelId)) {
            return Mono.empty();
        }
        return Mono.fromFuture(byLocalizedAndChannelCache.get(cacheKey(localizedTemplateId, channelId)))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Flux<ChannelTemplateEntity> listByLocalizedTemplateId(String localizedTemplateId) {
        if (!StringUtils.hasText(localizedTemplateId)) {
            return Flux.empty();
        }
        return channelTemplateRepository.findByLocalizedTemplateId(localizedTemplateId);
    }

    public Flux<ChannelTemplateEntity> listByChannelId(String channelId) {
        if (!StringUtils.hasText(channelId)) {
            return Flux.empty();
        }
        return channelTemplateRepository.findByChannelId(channelId);
    }

    public Mono<ChannelTemplateEntity> create(ChannelTemplateEntity entity) {
        if (entity == null
                || !StringUtils.hasText(entity.getLocalizedTemplateId())
                || !StringUtils.hasText(entity.getChannelId())) {
            return Mono.error(new BadRequestException("channelTemplate.key.empty"));
        }
        String localizedTemplateId = entity.getLocalizedTemplateId();
        String channelId = entity.getChannelId();
        return localizedTemplateRepository.findById(localizedTemplateId)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("channelTemplate.localized.notFound", localizedTemplateId)))
                .then(channelRepository.findById(channelId)
                        .switchIfEmpty(Mono.error(new NotFoundException("channelTemplate.channel.notFound", channelId))))
                .then(Mono.defer(() -> channelTemplateRepository
                        .findByLocalizedTemplateIdAndChannelId(localizedTemplateId, channelId)
                        .flatMap(existing -> Mono.<ChannelTemplateEntity>error(
                                new ConflictException("channelTemplate.exists")))
                        .switchIfEmpty(Mono.defer(() -> channelTemplateRepository.save(entity)
                                .doOnSuccess(this::invalidateCaches)))));
    }

    public Mono<ChannelTemplateEntity> update(String id, ChannelTemplateEntity patch) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("channelTemplate.id.empty"));
        }
        return channelTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("channelTemplate.notFound", id)))
                .flatMap(existing -> {
                    mergePatch(patch, existing);
                    return channelTemplateRepository.save(existing)
                            .doOnSuccess(this::invalidateCaches);
                });
    }

    public Mono<Void> deleteById(String id) {
        if (!StringUtils.hasText(id)) {
            return Mono.error(new BadRequestException("channelTemplate.id.empty"));
        }
        return channelTemplateRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("channelTemplate.notFound", id)))
                .flatMap(found -> channelTemplateRepository.deleteById(found.getId())
                        .doOnSuccess(v -> invalidateCaches(found)));
    }

    public Mono<Void> deleteByLocalizedTemplateId(String localizedTemplateId) {
        if (!StringUtils.hasText(localizedTemplateId)) {
            return Mono.empty();
        }
        return channelTemplateRepository.findByLocalizedTemplateId(localizedTemplateId)
                .doOnNext(this::invalidateCaches)
                .then(channelTemplateRepository.deleteByLocalizedTemplateId(localizedTemplateId));
    }

    public Mono<Void> deleteByChannelId(String channelId) {
        if (!StringUtils.hasText(channelId)) {
            return Mono.empty();
        }
        return channelTemplateRepository.findByChannelId(channelId)
                .doOnNext(this::invalidateCaches)
                .then(channelTemplateRepository.deleteByChannelId(channelId));
    }

    private void mergePatch(ChannelTemplateEntity patch, ChannelTemplateEntity target) {
        if (patch == null) {
            return;
        }
        if (patch.getChannelTemplateCode() != null) {
            target.setChannelTemplateCode(patch.getChannelTemplateCode());
        }
        if (patch.getUploadedAt() != null) {
            target.setUploadedAt(patch.getUploadedAt());
        }
        if (patch.getFailureReason() != null) {
            target.setFailureReason(patch.getFailureReason());
        }
    }

    private void invalidateCaches(ChannelTemplateEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() != null) {
            byIdCache.synchronous().invalidate(entity.getId());
        }
        if (StringUtils.hasText(entity.getLocalizedTemplateId())
                && StringUtils.hasText(entity.getChannelId())) {
            byLocalizedAndChannelCache.synchronous()
                    .invalidate(cacheKey(entity.getLocalizedTemplateId(), entity.getChannelId()));
        }
    }

    private String cacheKey(String localizedTemplateId, String channelId) {
        return localizedTemplateId + "|" + channelId;
    }
}
