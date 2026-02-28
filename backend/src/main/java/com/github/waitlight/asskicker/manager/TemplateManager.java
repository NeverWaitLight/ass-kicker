package com.github.waitlight.asskicker.manager;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

@Component
public class TemplateManager {

    private final MustacheFactory mustacheFactory = new DefaultMustacheFactory();

    private final TemplateService templateService;

    public TemplateManager(TemplateService templateService) {
        this.templateService = templateService;
    }

    public Mono<String> fill(String templateCode, Language language, Map<String, Object> params) {
        Map<String, Object> safeParams = params != null ? params : Collections.emptyMap();
        return templateService.findByCode(templateCode)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateCode)))
                .flatMap(template -> templateService.getTemplateContentByLanguage(template.getId(), language)
                        .switchIfEmpty(Mono.error(
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Language template not found")))
                        .map(lt -> render(lt.getContent(), safeParams)));
    }

    private String render(String content, Map<String, Object> params) {
        if (content == null) {
            return "";
        }
        Mustache mustache = mustacheFactory.compile(new StringReader(content), "tpl");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, params);
        return writer.toString();
    }
}
