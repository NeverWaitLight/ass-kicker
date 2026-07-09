package com.github.waitlight.asskicker.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig.CaffeineCacheProperties;
import com.github.waitlight.asskicker.model.Language;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TemplateEngine {

    private final TemplateService templateService;
    private final LocalizedTemplateService localizedTemplateService;
    private final MustacheFactory mustacheFactory;
    private final Cache<String, Mustache> compiledTemplateCache;

    public TemplateEngine(TemplateService templateService,
            LocalizedTemplateService localizedTemplateService,
            CaffeineCacheProperties cacheProperties) {
        this.templateService = templateService;
        this.localizedTemplateService = localizedTemplateService;
        this.mustacheFactory = new DefaultMustacheFactory();
        this.compiledTemplateCache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getMaximumSize())
                .expireAfterWrite(cacheProperties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                .build();
    }

    public <T extends SendReq> Mono<T> fill(T req) {
        if (req.isDirectSend() || req.getTemplateCode() == null || req.getTemplateCode().isBlank()) {
            return Mono.just(req);
        }
        return templateService.findByCode(req.getTemplateCode())
                .flatMap(tpl -> {
                    if (tpl.isProviderManaged()) {
                        req.applyRendered("", "");
                        return Mono.just(req);
                    }
                    return localizedTemplateService.findLocalized(tpl.getId(), req.getLanguage())
                            .map(lt -> {
                                Map<String, Object> params = toObjectMap(req.getTemplateParams());
                                String title = renderTemplate(req.getTemplateCode(), req.getLanguage(), lt.getTitle(), params);
                                String content = renderTemplate(req.getTemplateCode(), req.getLanguage(), lt.getContent(), params);
                                req.applyRendered(title, content);
                                return req;
                            });
                });
    }

    /**
     * 在持久层更新某模板某语言正文后可调用，按 templateCode 与 language 前缀清理已编译 Mustache，避免长期堆积旧版本条目
     */
    public void invalidateCompiledTemplates(String templateCode, Language language) {
        String prefix = engineTplName(templateCode, language) + "@";
        compiledTemplateCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    private String renderTemplate(String templateCode, Language language, String templateContent,
                                  Map<String, Object> templateParams) {
        String body = templateContent != null ? templateContent : "";
        String cacheKey = cacheKey(templateCode, language, body);
        Mustache mustache = compiledTemplateCache.get(cacheKey,
                k -> mustacheFactory.compile(new StringReader(body), k));
        StringWriter writer = new StringWriter();
        Map<String, Object> params = templateParams != null ? templateParams : Collections.emptyMap();
        mustache.execute(writer, params);
        return writer.toString();
    }

    private Map<String, Object> toObjectMap(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> copy = new LinkedHashMap<>(params.size());
        copy.putAll(params);
        return copy;
    }

    private String cacheKey(String templateCode, Language language, String templateContent) {
        return engineTplName(templateCode, language) + "@" + Integer.toHexString(templateContent.hashCode());
    }

    private String engineTplName(String templateCode, Language language) {
        String name = "tpl-" + templateCode + "-" + language.name();
        return name.toLowerCase();
    }
}
