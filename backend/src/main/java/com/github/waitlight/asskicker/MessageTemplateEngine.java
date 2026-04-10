package com.github.waitlight.asskicker;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheProperties;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.MessageTemplateService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageTemplateEngine {

    private final MessageTemplateService messageTemplateService;
    private final MustacheFactory mustacheFactory;
    private final Cache<String, Mustache> compiledTemplateCache;

    public MessageTemplateEngine(MessageTemplateService messageTemplateService,
            CaffeineCacheProperties cacheProperties) {
        this.messageTemplateService = messageTemplateService;
        this.mustacheFactory = new DefaultMustacheFactory();
        this.compiledTemplateCache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getMaximumSize())
                .expireAfterWrite(cacheProperties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                .build();
    }

    public Mono<UniMessage> fill(UniMessage req) {
        return messageTemplateService.findByCode(req.getTemplateCode())
                .flatMap(tpl -> Mono.justOrEmpty(
                        tpl.getLocalizedTemplates() != null
                                ? tpl.getLocalizedTemplates().get(req.getLanguage())
                                : null))
                .map(tpl -> {
                    String content = fill(req.getTemplateCode(), req.getLanguage(), tpl.getContent(),
                            req.getTemplateParams());
                    UniMessage uniMessage = new UniMessage();
                    uniMessage.setTemplateCode(req.getTemplateCode());
                    uniMessage.setLanguage(req.getLanguage());
                    uniMessage.setTitle(tpl.getTitle());
                    uniMessage.setContent(content);
                    uniMessage.setExtraData(req.getExtraData());
                    uniMessage.setTemplateParams(req.getTemplateParams() == null
                            ? Collections.emptyMap()
                            : Map.copyOf(req.getTemplateParams()));
                    return uniMessage;
                });
    }

    /**
     * 在持久层更新某模板某语言正文后可调用，按 templateCode 与 language 前缀清理已编译 Mustache，避免长期堆积旧版本条目
     */
    public void invalidateCompiledTemplates(String templateCode, Language language) {
        String prefix = engineTplName(templateCode, language) + "@";
        compiledTemplateCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    private String fill(String templateCode, Language language, String templateContent,
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

    private String cacheKey(String templateCode, Language language, String templateContent) {
        return engineTplName(templateCode, language) + "@" + Integer.toHexString(templateContent.hashCode());
    }

    private String engineTplName(String templateCode, Language language) {
        String name = "tpl-" + templateCode + "-" + language.name();
        return name.toLowerCase();
    }
}
