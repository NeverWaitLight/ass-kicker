package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LanguageTemplateService {
    Flux<LanguageTemplate> findAllByTemplateId(Long templateId);
    Mono<LanguageTemplate> findByTemplateIdAndLanguage(Long templateId, Language language);
    Mono<LanguageTemplate> save(LanguageTemplate languageTemplate);
    Mono<LanguageTemplate> update(Long id, LanguageTemplate languageTemplate);
    Mono<Void> deleteById(Long id);
}