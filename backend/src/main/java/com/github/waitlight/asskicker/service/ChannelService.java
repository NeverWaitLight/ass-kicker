package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import com.github.waitlight.asskicker.util.SoftDeleteConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelProviderConverter channelProviderConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public Flux<ChannelEntity> findAll() {
        return channelRepository.findAll();
    }

    public Flux<ChannelEntity> findEnabled() {
        return channelRepository.findByEnabled(true);
    }

    public Flux<ChannelEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return channelRepository.findAll()
                .skip(offset)
                .take(size);
    }

    public Mono<PageResp<ChannelProviderDTO>> page(int page, int size) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int zeroBasedPage = normalizedPage - 1;
        Mono<Long> totalMono = channelRepository.count(null);
        Mono<List<ChannelProviderDTO>> itemsMono =
                findAll(zeroBasedPage, normalizedSize)
                        .map(channelProviderConverter::toDto)
                        .collectList();
        return Mono.zip(itemsMono, totalMono)
                .map(t -> PageResp.success(normalizedPage, normalizedSize, t.getT2(), t.getT1()));
    }

    public Mono<ChannelEntity> findById(String id) {
        return channelRepository.findById(id);
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
        ChannelEntity toCreate = channelProviderConverter.copyForCreate(entity);
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
                .flatMap(existing -> ensureUniqueKey(patch.getCode(), id)
                        .then(Mono.defer(() -> {
                            channelProviderConverter.merge(patch, existing);
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
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Channel provider key already exists: " + key));
                })
                .then();
    }
}
