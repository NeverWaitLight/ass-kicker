package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.service.LanguageTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class LanguageTemplateServiceImpl implements LanguageTemplateService {

    @Autowired
    private LanguageTemplateRepository languageTemplateRepository;

    @Override
    public Flux<LanguageTemplate> findAllByTemplateId(String templateId) {
        return languageTemplateRepository.findByTemplateId(templateId);
    }

    @Override
    public Mono<LanguageTemplate> findByTemplateIdAndLanguage(String templateId, Language language) {
        return languageTemplateRepository.findByTemplateIdAndLanguage(templateId, language);
    }

    @Override
    public Mono<LanguageTemplate> save(LanguageTemplate languageTemplate) {
        languageTemplate.setId(null);
        long timestamp = Instant.now().toEpochMilli();
        languageTemplate.setCreatedAt(timestamp);
        languageTemplate.setUpdatedAt(timestamp);
        return languageTemplateRepository.save(languageTemplate);
    }

    @Override
    public Mono<LanguageTemplate> update(String id, LanguageTemplate languageTemplate) {
        return languageTemplateRepository.findById(id)
                .flatMap(existingLT -> {
                    existingLT.setLanguage(languageTemplate.getLanguage());
                    existingLT.setContent(languageTemplate.getContent());
                    existingLT.setUpdatedAt(Instant.now().toEpochMilli());
                    return languageTemplateRepository.save(existingLT);
                });
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return languageTemplateRepository.deleteById(id);
    }
}
