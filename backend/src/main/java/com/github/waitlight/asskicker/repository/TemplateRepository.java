package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.TemplateEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TemplateRepository extends ReactiveMongoRepository<TemplateEntity, String> {
    Mono<TemplateEntity> findByCode(String code);
}
