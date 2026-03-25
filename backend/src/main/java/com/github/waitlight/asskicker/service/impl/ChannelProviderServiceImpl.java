package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.channelprovider.ChannelProviderDTO;
import com.github.waitlight.asskicker.dto.common.PageResp;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.repository.ChannelProviderRepository;
import com.github.waitlight.asskicker.service.ChannelProviderService;
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
public class ChannelProviderServiceImpl implements ChannelProviderService {

    private final ChannelProviderRepository channelProviderRepository;
    private final ChannelProviderConverter channelProviderConverter;

    @Override
    public Flux<ChannelProviderEntity> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return channelProviderRepository.findAll()
                .skip(offset)
                .take(size);
    }

    @Override
    public Mono<PageResp<ChannelProviderDTO>> listPage(int page, int size) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int zeroBasedPage = normalizedPage - 1;
        Mono<Long> totalMono = channelProviderRepository.count();
        Mono<List<ChannelProviderDTO>> itemsMono =
                findAll(zeroBasedPage, normalizedSize)
                        .map(channelProviderConverter::toDto)
                        .collectList();
        return Mono.zip(itemsMono, totalMono)
                .map(t -> new PageResp<>(t.getT1(), normalizedPage, normalizedSize, t.getT2()));
    }

    @Override
    public Mono<ChannelProviderEntity> findById(String id) {
        return channelProviderRepository.findById(id);
    }

    @Override
    public Mono<ChannelProviderEntity> findByKey(String key) {
        return channelProviderRepository.findByKey(key);
    }

    @Override
    public Flux<ChannelProviderEntity> findByType(ChannelType type) {
        return channelProviderRepository.findByType(type);
    }

    @Override
    public Flux<ChannelProviderEntity> findEnabledByType(ChannelType type) {
        return channelProviderRepository.findByTypeAndEnabled(type, true);
    }

    @Override
    public Mono<ChannelProviderEntity> create(ChannelProviderEntity entity) {
        ChannelProviderEntity toCreate = channelProviderConverter.copyForCreate(entity);
        toCreate.setId(null);
        long now = Instant.now().toEpochMilli();
        toCreate.setCreatedAt(now);
        toCreate.setUpdatedAt(now);
        return ensureUniqueKey(toCreate.getKey(), null)
                .then(Mono.defer(() -> channelProviderRepository.save(toCreate)));
    }

    @Override
    public Mono<ChannelProviderEntity> update(String id, ChannelProviderEntity patch) {
        return channelProviderRepository.findById(id)
                .flatMap(existing -> ensureUniqueKey(patch.getKey(), id)
                        .then(Mono.defer(() -> {
                            channelProviderConverter.merge(patch, existing);
                            existing.setUpdatedAt(Instant.now().toEpochMilli());
                            return channelProviderRepository.save(existing);
                        })));
    }

    @Override
    public Mono<Void> delete(String id) {
        return channelProviderRepository.deleteById(id);
    }

    private Mono<Void> ensureUniqueKey(String key, String currentId) {
        return channelProviderRepository.findByKey(key)
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
