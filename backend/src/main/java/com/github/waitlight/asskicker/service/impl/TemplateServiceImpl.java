package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import com.github.waitlight.asskicker.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.time.Instant;

@Service
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private LanguageTemplateRepository languageTemplateRepository;

    @Override
    public Flux<Template> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.empty();
        }
        long offset = (long) page * (long) size;
        return templateRepository.findAll()
                .skip(offset)
                .take(size);
    }

    @Override
    public Mono<Template> findById(String id) {
        return templateRepository.findById(id);
    }

    @Override
    public Mono<Template> findByCode(String code) {
        return templateRepository.findByCode(code);
    }

    @Override
    public Mono<Template> createTemplate(Template template) {
        template.setId(null);
        long timestamp = Instant.now().toEpochMilli();
        template.setCreatedAt(timestamp);
        template.setUpdatedAt(timestamp);
        return templateRepository.save(template);
    }

    @Override
    public Mono<Template> updateTemplate(String id, Template template) {
        return templateRepository.findById(id)
                .flatMap(existingTemplate -> {
                    existingTemplate.setName(template.getName());
                    existingTemplate.setCode(template.getCode());
                    existingTemplate.setDescription(template.getDescription());
                    existingTemplate.setApplicableChannelTypes(template.getApplicableChannelTypes());
                    existingTemplate.setContentType(template.getContentType());
                    existingTemplate.setUpdatedAt(Instant.now().toEpochMilli());
                    return templateRepository.save(existingTemplate);
                });
    }

    @Override
    public Mono<Void> deleteTemplate(String id) {
        return templateRepository.deleteById(id);
    }

    @Override
    public Mono<Template> getTemplateById(String id) {
        return templateRepository.findById(id);
    }

    @Override
    public Mono<LanguageTemplate> getTemplateContentByLanguage(String templateId, Language language) {
        return languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language);
    }

    @Override
    public Mono<LanguageTemplate> saveTemplateContentByLanguage(String templateId, Language language, String content) {
        return templateRepository.findById(templateId)
                .flatMap(ignored -> languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                        .flatMap(existingLT -> {
                            existingLT.setContent(content);
                            existingLT.setUpdatedAt(Instant.now().toEpochMilli());
                            return languageTemplateRepository.save(existingLT);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            long timestamp = Instant.now().toEpochMilli();
                            LanguageTemplate created = new LanguageTemplate(templateId, language, content);
                            created.setCreatedAt(timestamp);
                            created.setUpdatedAt(timestamp);
                            return languageTemplateRepository.save(created);
                        }))
                );
    }

    @Override
    public Flux<LanguageTemplate> getAllTemplateContentsByTemplateId(String templateId) {
        return languageTemplateRepository.findByTemplateId(templateId);
    }

    @Override
    public Mono<Void> deleteTemplateContentByLanguage(String templateId, Language language) {
        return languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language)
                .flatMap(languageTemplateRepository::delete);
    }
}
