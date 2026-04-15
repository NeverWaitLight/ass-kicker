package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.converter.ChannelConverter;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelConverter channelConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<ChannelEntity>> channelByIdCache;
    private AsyncLoadingCache<String, Optional<ChannelEntity>> channelByKeyCache;

    @PostConstruct
    void initCaches() {
        channelByIdCache = caffeineCacheConfig.buildCache((id, executor) -> channelRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        channelByKeyCache = caffeineCacheConfig.buildCache((key, executor) -> channelRepository.findByCode(key)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());
    }

    public Flux<ChannelEntity> findAll() {
        return channelRepository.findAll();
    }

    public Flux<ChannelEntity> findEnabled() {
        return channelRepository.findByEnabled(true);
    }

    public Mono<Long> count(String keyword) {
        return channelRepository.count(keyword);
    }

    public Flux<ChannelEntity> list(String keyword, int limit, int offset) {
        return channelRepository.list(keyword, limit, offset);
    }

    public Mono<ChannelEntity> findById(String id) {
        return Mono.fromFuture(channelByIdCache.get(id))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<ChannelEntity> getById(String id) {
        return Mono.fromFuture(channelByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new NotFoundException("channel.notFound", new Object[] { id }))));
    }

    public Mono<ChannelEntity> findByKey(String key) {
        return Mono.fromFuture(channelByKeyCache.get(key))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Flux<ChannelEntity> findByType(ChannelType type) {
        return channelRepository.findByChannelType(type);
    }

    public Flux<ChannelEntity> findEnabledByType(ChannelType type) {
        return channelRepository.findByChannelTypeAndEnabled(type, true);
    }

    public Mono<ChannelEntity> create(ChannelEntity entity) {
        ChannelEntity toCreate = channelConverter.copyForCreate(entity);
        toCreate.setId(null);
        long now = Instant.now().toEpochMilli();
        toCreate.setId(snowflakeIdGenerator.nextIdString());
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        toCreate.setDeletedAt(SoftDeleteConstants.NOT_DELETED);
        return ensureUniqueKey(toCreate.getCode(), null)
                .then(Mono.defer(() -> channelRepository.save(toCreate)))
                .doOnSuccess(saved -> {
                    if (saved != null) {
                        invalidateChannelCaches(saved.getId(), saved.getCode());
                    }
                });
    }

    public Mono<ChannelEntity> update(String id, ChannelEntity patch) {
        return channelRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("channel.notFound", new Object[] { id })))
                .flatMap(existing -> {
                    String oldCode = existing.getCode();
                    return ensureUniqueKey(patch.getCode(), id)
                            .then(Mono.defer(() -> {
                                channelConverter.merge(patch, existing);
                                existing.setUpdatedAt(Instant.now().toEpochMilli());
                                return channelRepository.save(existing)
                                        .doOnSuccess(saved -> {
                                            if (saved != null) {
                                                invalidateChannelCaches(saved.getId(), oldCode, saved.getCode());
                                            }
                                        });
                            }));
                });
    }

    public Mono<Void> delete(String id) {
        return channelRepository.findById(id)
                .flatMap(existing -> channelRepository.deleteById(id)
                        .doOnSuccess(v -> invalidateChannelCaches(existing.getId(), existing.getCode())))
                .then();
    }

    private void invalidateChannelCaches(String id, String code) {
        channelByIdCache.synchronous().invalidate(id);
        if (code != null) {
            channelByKeyCache.synchronous().invalidate(code);
        }
    }

    private void invalidateChannelCaches(String id, String oldCode, String newCode) {
        channelByIdCache.synchronous().invalidate(id);
        if (oldCode != null) {
            channelByKeyCache.synchronous().invalidate(oldCode);
        }
        if (newCode != null && !newCode.equals(oldCode)) {
            channelByKeyCache.synchronous().invalidate(newCode);
        }
    }

    private Mono<Void> ensureUniqueKey(String key, String currentId) {
        return channelRepository.findByCode(key)
                .flatMap(existing -> {
                    if (currentId != null && currentId.equals(existing.getId())) {
                        return Mono.empty();
                    }
                    return Mono.error(new ConflictException("channel.key.exists", new Object[] { key }));
                })
                .then();
    }
}
