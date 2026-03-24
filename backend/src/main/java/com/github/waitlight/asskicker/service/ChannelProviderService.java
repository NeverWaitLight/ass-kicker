package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelProviderService {

    Flux<ChannelProviderEntity> findAll(int page, int size);

    Mono<ChannelProviderEntity> findById(String id);

    Mono<ChannelProviderEntity> findByKey(String key);

    Flux<ChannelProviderEntity> findByType(ChannelType type);

    Flux<ChannelProviderEntity> findEnabledByType(ChannelType type);

    Mono<ChannelProviderEntity> create(ChannelProviderEntity entity);

    Mono<ChannelProviderEntity> update(String id, ChannelProviderEntity entity);

    Mono<Void> delete(String id);
}
