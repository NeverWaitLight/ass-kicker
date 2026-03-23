package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LanguageTemplateService {
    Flux<LanguageTemplateEntity> findAllByTemplateId(String templateId);

    Mono<LanguageTemplateEntity> findByTemplateIdAndLanguage(String templateId, Language language);

    Mono<LanguageTemplateEntity> save(LanguageTemplateEntity languageTemplate);

    Mono<LanguageTemplateEntity> update(String id, LanguageTemplateEntity languageTemplate);

    Mono<Void> deleteById(String id);
}
