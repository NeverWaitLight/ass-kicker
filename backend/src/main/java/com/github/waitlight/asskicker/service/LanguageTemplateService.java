package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LanguageTemplateService {
    Flux<LanguageTemplate> findAllByTemplateId(String templateId);
    Mono<LanguageTemplate> findByTemplateIdAndLanguage(String templateId, Language language);
    Mono<LanguageTemplate> save(LanguageTemplate languageTemplate);
    Mono<LanguageTemplate> update(String id, LanguageTemplate languageTemplate);
    Mono<Void> deleteById(String id);
}
