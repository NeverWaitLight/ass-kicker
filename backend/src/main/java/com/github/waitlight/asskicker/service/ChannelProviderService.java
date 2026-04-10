package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;
import com.github.waitlight.asskicker.dto.PageResult;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
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
public class ChannelProviderService {

    private final ChannelProviderRepository channelProviderRepository;
    private final ChannelProviderConverter channelProviderConverter;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public Flux<ChannelProviderEntity> findAll() {
        return channelProviderRepository.findAll();
    }

    public Flux<ChannelProviderEntity> findEnabled() {
        return channelProviderRepository.findByEnabled(true);
    }

    public Flux<ChannelProviderEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return channelProviderRepository.findAll()
                .skip(offset)
                .take(size);
    }

    public Mono<PageResult<ChannelProviderDTO>> listPage(int page, int size) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int zeroBasedPage = normalizedPage - 1;
        Mono<Long> totalMono = channelProviderRepository.count();
        Mono<List<ChannelProviderDTO>> itemsMono =
                findAll(zeroBasedPage, normalizedSize)
                        .map(channelProviderConverter::toDto)
                        .collectList();
        return Mono.zip(itemsMono, totalMono)
                .map(t -> new PageResult<>(normalizedPage, normalizedSize, t.getT2(), t.getT1()));
    }

    public Mono<ChannelProviderEntity> findById(String id) {
        return channelProviderRepository.findById(id);
    }

    public Mono<ChannelProviderEntity> findByKey(String key) {
        return channelProviderRepository.findByCode(key);
    }

    public Flux<ChannelProviderEntity> findByType(ChannelType type) {
        return channelProviderRepository.findByChannelType(type);
    }

    public Flux<ChannelProviderEntity> findEnabledByType(ChannelType type) {
        return channelProviderRepository.findByChannelTypeAndEnabled(type, true);
    }

    public Mono<ChannelProviderEntity> create(ChannelProviderEntity entity) {
        ChannelProviderEntity toCreate = channelProviderConverter.copyForCreate(entity);
        toCreate.setId(null);
        long now = Instant.now().toEpochMilli();
        toCreate.setId(snowflakeIdGenerator.nextIdString());
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        return ensureUniqueKey(toCreate.getCode(), null)
                .then(Mono.defer(() -> channelProviderRepository.save(toCreate)));
    }

    public Mono<ChannelProviderEntity> update(String id, ChannelProviderEntity patch) {
        return channelProviderRepository.findById(id)
                .flatMap(existing -> ensureUniqueKey(patch.getCode(), id)
                        .then(Mono.defer(() -> {
                            channelProviderConverter.merge(patch, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return channelProviderRepository.save(existing);
                        })));
    }

    public Mono<Void> delete(String id) {
        return channelProviderRepository.deleteById(id);
    }

    private Mono<Void> ensureUniqueKey(String key, String currentId) {
        return channelProviderRepository.findByCode(key)
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
