package com.github.waitlight.asskicker.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.CaffeineCacheProperties;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.Language;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TemplateEngine {

    private static final Pattern MUSTACHE_VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final TemplateService templateService;
    private final MustacheFactory mustacheFactory;
    private final Cache<String, Mustache> compiledTemplateCache;

    public TemplateEngine(TemplateService templateService,
            CaffeineCacheProperties cacheProperties) {
        this.templateService = templateService;
        this.mustacheFactory = new DefaultMustacheFactory();
        this.compiledTemplateCache = Caffeine.newBuilder()
                .maximumSize(cacheProperties.getMaximumSize())
                .expireAfterWrite(cacheProperties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
                .build();
    }

    /**
     * @deprecated 改用 {@link #fill(SendReq)}
     */
    @Deprecated
    public Mono<UniMessage> fillold(UniMessage req) {
        if (req.isDirectSend() || req.getTemplateCode() == null || req.getTemplateCode().isBlank()) {
            return Mono.just(req);
        }
        return templateService.findByCode(req.getTemplateCode())
                .flatMap(tpl -> {
                    if (tpl.isProviderManaged()) {
                        return Mono.just(buildUniMessage(req, "", "", req.getTemplateParams()));
                    }
                    return Mono.justOrEmpty(
                            tpl.getLocalizedTemplates() != null
                                    ? tpl.getLocalizedTemplates().get(req.getLanguage())
                                    : null)
                            .map(lt -> {
                                Map<String, Object> params = req.getTemplateParams() != null
                                        ? req.getTemplateParams()
                                        : Collections.emptyMap();
                                String title = fillold(req.getTemplateCode(), req.getLanguage(), lt.getTitle(), params);
                                String content = fillold(req.getTemplateCode(), req.getLanguage(), lt.getContent(), params);
                                return buildUniMessage(req, title, content, params);
                            });
                });
    }

    /**
     * @deprecated 随 {@link UniMessage} 废弃而废弃。
     */
    @Deprecated
    private UniMessage buildUniMessage(UniMessage req, String title, String content, Map<String, Object> params) {
        UniMessage uniMessage = new UniMessage();
        uniMessage.setTemplateCode(req.getTemplateCode());
        uniMessage.setLanguage(req.getLanguage());
        uniMessage.setTitle(title);
        uniMessage.setContent(content);
        uniMessage.setExtraData(req.getExtraData());
        uniMessage.setTemplateParams(params);
        return uniMessage;
    }

    public <T extends SendReq> Mono<T> fill(T req) {
        UniMessage msg = toUniMessage(req);
        return fillold(msg)
                .map(filled -> {
                    req.applyRendered(filled.getTitle(), filled.getContent());
                    return req;
                });
    }

    private UniMessage toUniMessage(SendReq req) {
        UniMessage msg = new UniMessage();
        msg.setTemplateCode(req.getTemplateCode());
        msg.setLanguage(req.getLanguage());
        msg.setDirectSend(req.isDirectSend());
        if (req.getTemplateParams() != null) {
            msg.setTemplateParams(new HashMap<>(req.getTemplateParams()));
        }
        return msg;
    }

    /**
     * @deprecated 随 {@link UniMessage} 废弃而废弃。
     */
    @Deprecated
    public Mono<Set<String>> findMissingVariables(UniMessage req) {
        return templateService.findByCode(req.getTemplateCode())
                .flatMap(tpl -> Mono.justOrEmpty(
                        tpl.getLocalizedTemplates() != null
                                ? tpl.getLocalizedTemplates().get(req.getLanguage())
                                : null))
                .map(tpl -> {
                    Map<String, Object> params = req.getTemplateParams() != null
                            ? req.getTemplateParams()
                            : Collections.emptyMap();
                    Set<String> variables = extractVariableNames(tpl.getTitle());
                    variables.addAll(extractVariableNames(tpl.getContent()));
                    Set<String> missing = new LinkedHashSet<>();
                    for (String variable : variables) {
                        if (!isVariableFilled(params, variable)) {
                            missing.add(variable);
                        }
                    }
                    return Collections.unmodifiableSet(missing);
                });
    }

    /**
     * 在持久层更新某模板某语言正文后可调用，按 templateCode 与 language 前缀清理已编译 Mustache，避免长期堆积旧版本条目
     */
    public void invalidateCompiledTemplates(String templateCode, Language language) {
        String prefix = engineTplName(templateCode, language) + "@";
        compiledTemplateCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    private String fillold(String templateCode, Language language, String templateContent,
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

    private Set<String> extractVariableNames(String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = MUSTACHE_VARIABLE_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private boolean isVariableFilled(Map<String, Object> params, String variable) {
        if (params.containsKey(variable)) {
            return params.get(variable) != null;
        }
        if (!variable.contains(".")) {
            return false;
        }
        Object current = params;
        for (String part : variable.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return false;
            }
            current = map.get(part);
        }
        return current != null;
    }

    private String cacheKey(String templateCode, Language language, String templateContent) {
        return engineTplName(templateCode, language) + "@" + Integer.toHexString(templateContent.hashCode());
    }

    private String engineTplName(String templateCode, Language language) {
        String name = "tpl-" + templateCode + "-" + language.name();
        return name.toLowerCase();
    }
}
