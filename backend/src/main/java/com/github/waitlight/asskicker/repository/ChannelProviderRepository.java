package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChannelProviderRepository extends ReactiveMongoRepository<ChannelProviderEntity, String> {

    Mono<ChannelProviderEntity> findByCode(String code);

    Flux<ChannelProviderEntity> findByChannelType(ChannelType channelType);

    Flux<ChannelProviderEntity> findByEnabled(boolean enabled);

    Flux<ChannelProviderEntity> findByChannelTypeAndEnabled(ChannelType channelType, boolean enabled);
}
