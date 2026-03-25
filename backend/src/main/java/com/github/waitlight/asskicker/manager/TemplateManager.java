package com.github.waitlight.asskicker.manager;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.config.CaffeineCacheProperties;
import com.github.waitlight.asskicker.dto.template.FilledTemplateResult;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.TemplateService;

import reactor.core.publisher.Mono;

@Deprecated
@Component
public class TemplateManager {

    private final TemplateService templateService;
    private final MustacheFactory mustacheFactory;
    private final Cache<String, Mustache> compiledTemplateCache;
    private final AtomicLong templateSequence = new AtomicLong();

    @Autowired
    public TemplateManager(TemplateService templateService, CaffeineCacheProperties cacheProperties) {
        this(templateService, new DefaultMustacheFactory(), Caffeine.newBuilder()
                .maximumSize(cacheProperties.getMaximumSize())
                .expireAfterAccess(cacheProperties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                .build());
    }

    TemplateManager(TemplateService templateService, MustacheFactory mustacheFactory,
            Cache<String, Mustache> compiledTemplateCache) {
        this.templateService = templateService;
        this.mustacheFactory = mustacheFactory;
        this.compiledTemplateCache = compiledTemplateCache;
    }

    public Mono<FilledTemplateResult> fill(String templateCode, Language language, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        return templateService.findByCode(templateCode)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateCode)))
                .flatMap(template -> templateService.getTemplateContentByLanguage(template.getId(), language)
                        .switchIfEmpty(Mono.error(
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Language template not found")))
                        .map(lt -> new FilledTemplateResult(template, render(lt.getContent(), safeParams))));
    }

    private String render(String content, Map<String, Object> params) {
        if (content == null) {
            return "";
        }
        Mustache mustache = compiledTemplateCache.get(content, this::compileTemplate);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, params);
        return writer.toString();
    }

    private Mustache compileTemplate(String content) {
        String templateName = "tpl-" + templateSequence.incrementAndGet();
        return mustacheFactory.compile(new StringReader(content), templateName);
    }
}
