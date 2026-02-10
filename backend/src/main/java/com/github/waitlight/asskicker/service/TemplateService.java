package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.Template;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TemplateService {
    Flux<Template> findAll(int page, int size);
    Mono<Template> findById(Long id);
    Mono<Template> findByCode(String code);
    Mono<Template> createTemplate(Template template);
    Mono<Template> updateTemplate(String id, Template template);
    Mono<Void> deleteTemplate(String id);
    Mono<Template> getTemplateById(String id);

    // Methods for managing language-specific templates
    Mono<LanguageTemplate> getTemplateContentByLanguage(Long templateId, Language language);
    Mono<LanguageTemplate> saveTemplateContentByLanguage(Long templateId, Language language, String content);
    Flux<LanguageTemplate> getAllTemplateContentsByTemplateId(Long templateId);
    Mono<Void> deleteTemplateContentByLanguage(Long templateId, Language language);
}