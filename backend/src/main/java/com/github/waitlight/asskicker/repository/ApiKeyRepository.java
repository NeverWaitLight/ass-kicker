package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.ApiKeyEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ApiKeyRepository extends ReactiveMongoRepository<ApiKeyEntity, String> {

    Mono<ApiKeyEntity> findByKeyPrefix(String keyPrefix);

    Flux<ApiKeyEntity> findByUserId(String userId);
}
