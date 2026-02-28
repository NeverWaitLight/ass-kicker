package com.github.waitlight.asskicker.manager;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateManager {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private final TemplateService templateService;

    public TemplateManager(TemplateService templateService) {
        this.templateService = templateService;
    }

    public Mono<String> fill(String templateCode, Language language, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        return templateService.findByCode(templateCode)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateCode)))
                .flatMap(template -> templateService.getTemplateContentByLanguage(template.getId(), language)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Language template not found")))
                        .map(lt -> render(lt.getContent(), safeParams)));
    }

    private String render(String content, Map<String, Object> params) {
        if (content == null) {
            return "";
        }
        if (params == null || params.isEmpty()) {
            return content;
        }
        Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = params.get(key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
