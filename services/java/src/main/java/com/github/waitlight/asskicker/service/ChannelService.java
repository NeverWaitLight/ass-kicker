package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<ChannelEntity>> channelByIdCache;
    private AsyncLoadingCache<String, Optional<ChannelEntity>> channelByCodeCache;

    @PostConstruct
    void initCaches() {
        channelByIdCache = caffeineCacheConfig.buildCache((id, executor) -> channelRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());

        channelByCodeCache = caffeineCacheConfig.buildCache((code, executor) -> channelRepository.findByCode(code)
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

    public Mono<Long> count(String keyword, ChannelType channelType, ChannelProvider provider) {
        return channelRepository.count(keyword, channelType, provider);
    }

    public Flux<ChannelEntity> list(String keyword, ChannelType channelType, ChannelProvider provider, int limit,
            int offset) {
        return channelRepository.list(keyword, channelType, provider, limit, offset);
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

    public Mono<ChannelEntity> findByCode(String code) {
        return Mono.fromFuture(channelByCodeCache.get(code))
                .flatMap(opt -> opt.map(Mono::just).orElse(Mono.empty()));
    }

    public Flux<ChannelEntity> findByType(ChannelType type) {
        return channelRepository.findByChannelType(type);
    }

    public Flux<ChannelEntity> findEnabledByType(ChannelType type) {
        return channelRepository.findByChannelTypeAndEnabled(type, true);
    }

    public Mono<ChannelEntity> create(ChannelEntity c) {
        return ensureUniqueCode(c.getCode(), null)
                .then(Mono.defer(() -> channelRepository.save(c)))
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
                    return ensureUniqueCode(patch.getCode(), id)
                            .then(Mono.defer(() -> {
                                mergeEntity(patch, existing);
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

    /**
     * 合并 patch 到 target，忽略 null 值，保持 id、creator、createdAt、updatedAt 不变
     */
    private void mergeEntity(ChannelEntity patch, ChannelEntity target) {
        if (patch.getCode() != null) {
            target.setCode(patch.getCode());
        }
        if (patch.getName() != null) {
            target.setName(patch.getName());
        }
        if (patch.getType() != null) {
            target.setType(patch.getType());
        }
        if (patch.getProvider() != null) {
            target.setProvider(patch.getProvider());
        }
        if (patch.getDescription() != null) {
            target.setDescription(patch.getDescription());
        }
        if (patch.isEnabled() != target.isEnabled()) {
            target.setEnabled(patch.isEnabled());
        }
        if (patch.getProperties() != null) {
            target.setProperties(patch.getProperties());
        }
    }

    private void invalidateChannelCaches(String id, String code) {
        channelByIdCache.synchronous().invalidate(id);
        if (code != null) {
            channelByCodeCache.synchronous().invalidate(code);
        }
    }

    private void invalidateChannelCaches(String id, String oldCode, String newCode) {
        channelByIdCache.synchronous().invalidate(id);
        if (oldCode != null) {
            channelByCodeCache.synchronous().invalidate(oldCode);
        }
        if (newCode != null && !newCode.equals(oldCode)) {
            channelByCodeCache.synchronous().invalidate(newCode);
        }
    }

    private Mono<Void> ensureUniqueCode(String code, String currentId) {
        return channelRepository.findByCode(code)
                .flatMap(existing -> {
                    if (currentId != null && currentId.equals(existing.getId())) {
                        return Mono.empty();
                    }
                    return Mono.error(new ConflictException("channel.code.exists", new Object[] { code }));
                })
                .then();
    }
}
