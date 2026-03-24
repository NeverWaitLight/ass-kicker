package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChannelProviderRepository extends ReactiveMongoRepository<ChannelProviderEntity, String> {

    Mono<ChannelProviderEntity> findByKey(String key);

    Flux<ChannelProviderEntity> findByType(ChannelType type);

    Flux<ChannelProviderEntity> findByTypeAndEnabled(ChannelType type, boolean enabled);

    Flux<ChannelProviderEntity> findByProvider(String provider);
}
