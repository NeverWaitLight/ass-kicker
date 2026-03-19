package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LanguageTemplateRepository extends ReactiveMongoRepository<LanguageTemplateEntity, String> {
    Flux<LanguageTemplateEntity> findByTemplateId(String templateId);

    Mono<LanguageTemplateEntity> findByTemplateIdAndLanguage(String templateId, Language language);
}
