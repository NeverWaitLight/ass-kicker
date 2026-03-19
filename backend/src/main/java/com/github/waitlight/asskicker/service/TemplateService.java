package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TemplateService {

    Flux<TemplateEntity> findAll(int page, int size);
    Mono<TemplateEntity> findById(String id);
    Mono<TemplateEntity> findByCode(String code);
    Mono<TemplateEntity> createTemplate(TemplateEntity template);
    Mono<TemplateEntity> updateTemplate(String id, TemplateEntity template);
    Mono<Void> deleteTemplate(String id);
    Mono<TemplateEntity> getTemplateById(String id);

    Mono<LanguageTemplateEntity> getTemplateContentByLanguage(String templateId, Language language);
    Mono<LanguageTemplateEntity> saveTemplateContentByLanguage(String templateId, Language language, String content);
    Flux<LanguageTemplateEntity> getAllTemplateContentsByTemplateId(String templateId);
    Mono<Void> deleteTemplateContentByLanguage(String templateId, Language language);
}
