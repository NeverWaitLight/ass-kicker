package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LanguageTemplateRepository extends ReactiveMongoRepository<LanguageTemplate, String> {
    Flux<LanguageTemplate> findByTemplateId(String templateId);

    Mono<LanguageTemplate> findByTemplateIdAndLanguage(String templateId, Language language);
}
