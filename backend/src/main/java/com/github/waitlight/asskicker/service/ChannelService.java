package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.converter.ChannelConverter;
import com.github.waitlight.asskicker.exception.ConflictException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelConverter channelConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

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
        return channelRepository.findById(id);
    }

    public Mono<ChannelEntity> getById(String id) {
        return channelRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("channel.notFound", new Object[] { id })));
    }

    public Mono<ChannelEntity> findByKey(String key) {
        return channelRepository.findByCode(key);
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
                .then(Mono.defer(() -> channelRepository.save(toCreate)));
    }

    public Mono<ChannelEntity> update(String id, ChannelEntity patch) {
        return channelRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("channel.notFound", new Object[] { id })))
                .flatMap(existing -> ensureUniqueKey(patch.getCode(), id)
                        .then(Mono.defer(() -> {
                            channelConverter.merge(patch, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return channelRepository.save(existing);
                        })));
    }

    public Mono<Void> delete(String id) {
        return channelRepository.deleteById(id);
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
