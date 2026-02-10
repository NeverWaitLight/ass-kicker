package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Template;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TemplateRepository extends ReactiveCrudRepository<Template, Long> {
    Mono<Template> findByCode(String code);
}
