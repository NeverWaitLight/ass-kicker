package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class TemplateHandler {

    private final TemplateService templateService;

    public TemplateHandler(TemplateService templateService) {
        this.templateService = templateService;
    }

    public Mono<ServerResponse> createTemplate(ServerRequest request) {
        return request.bodyToMono(Template.class)
                .map(this::sanitizeTemplate)
                .flatMap(template -> templateService.createTemplate(template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .onErrorResume(throwable -> {
                    // 记录错误日志
                    System.err.println("Error creating template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to create template"));
                });
    }

    public Mono<ServerResponse> getTemplateById(ServerRequest request) {
        String id = request.pathVariable("id");
        return templateService.getTemplateById(id)
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(throwable -> {
                    // 记录错误日志
                    System.err.println("Error retrieving template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to retrieve template"));
                });
    }

    public Mono<ServerResponse> getTemplateByCode(ServerRequest request) {
        String code = request.pathVariable("code");
        return templateService.findByCode(code)
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(throwable -> {
                    // 记录错误日志
                    System.err.println("Error retrieving template by code: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to retrieve template by code"));
                });
    }

    public Mono<ServerResponse> updateTemplate(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(Template.class)
                .map(this::sanitizeTemplate)
                .flatMap(template -> templateService.updateTemplate(id, template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(throwable -> {
                    // 记录错误日志
                    System.err.println("Error updating template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to update template"));
                });
    }

    public Mono<ServerResponse> deleteTemplate(ServerRequest request) {
        String id = request.pathVariable("id");
        return templateService.deleteTemplate(id)
                .then(ServerResponse.noContent().build())
                .onErrorResume(throwable -> {
                    // 记录错误日志
                    System.err.println("Error deleting template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    // 新增方法：获取特定语言的模板内容
    public Mono<ServerResponse> getTemplateContentByLanguage(ServerRequest request) {
        String templateIdStr = request.pathVariable("templateId");
        String languageCode = request.pathVariable("language");

        try {
            Long templateId = Long.parseLong(templateIdStr);
            Language language = Language.fromCode(languageCode);

            return templateService.getTemplateContentByLanguage(templateId, language)
                    .flatMap(content -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(content)))
                    .switchIfEmpty(ServerResponse.notFound().build())
                    .onErrorResume(throwable -> {
                        System.err.println("Error retrieving template content by language: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to retrieve template content by language"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid template ID format"));
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid language code"));
        }
    }

    // 新增方法：保存特定语言的模板内容
    public Mono<ServerResponse> saveTemplateContentByLanguage(ServerRequest request) {
        String templateIdStr = request.pathVariable("templateId");
        String languageCode = request.pathVariable("language");

        try {
            Long templateId = Long.parseLong(templateIdStr);
            Language language = Language.fromCode(languageCode);

            return request.bodyToMono(String.class)
                    .flatMap(content -> templateService.saveTemplateContentByLanguage(templateId, language, content))
                    .flatMap(content -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(content)))
                    .onErrorResume(throwable -> {
                        System.err.println("Error saving template content by language: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to save template content by language"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid template ID format"));
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid language code"));
        }
    }

    // 新增方法：获取模板的所有语言内容
    public Mono<ServerResponse> getAllTemplateContentsByTemplateId(ServerRequest request) {
        String templateIdStr = request.pathVariable("templateId");

        try {
            Long templateId = Long.parseLong(templateIdStr);

            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(templateService.getAllTemplateContentsByTemplateId(templateId), LanguageTemplate.class)
                    .onErrorResume(throwable -> {
                        System.err.println("Error retrieving all template contents: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to retrieve all template contents"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid template ID format"));
        }
    }

    public Mono<ServerResponse> handle(ServerRequest request) {
        // 默认处理方法，可以根据需要实现
        return ServerResponse.badRequest().build();
    }

    private Template sanitizeTemplate(Template input) {
        Template sanitized = new Template();
        if (input == null) {
            return sanitized;
        }
        sanitized.setName(input.getName());
        sanitized.setCode(input.getCode());
        sanitized.setDescription(input.getDescription());
        return sanitized;
    }
}
