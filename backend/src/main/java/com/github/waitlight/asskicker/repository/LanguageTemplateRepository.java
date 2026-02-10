package com.github.waitlight.asskicker.repository;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LanguageTemplateRepository extends ReactiveCrudRepository<LanguageTemplate, Long> {
    Flux<LanguageTemplate> findByTemplateId(Long templateId);

    Mono<LanguageTemplate> findByTemplateIdAndLanguage(Long templateId, Language language);
}