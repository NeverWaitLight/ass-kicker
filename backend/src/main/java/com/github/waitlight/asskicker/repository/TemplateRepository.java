package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Template;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TemplateRepository extends ReactiveMongoRepository<Template, String> {
    Mono<Template> findByCode(String code);
}
