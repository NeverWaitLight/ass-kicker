package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.service.LanguageTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class LanguageTemplateHandler {

    private final LanguageTemplateService languageTemplateService;

    public LanguageTemplateHandler(LanguageTemplateService languageTemplateService) {
        this.languageTemplateService = languageTemplateService;
    }

    public Mono<ServerResponse> getLanguageTemplatesByTemplateId(ServerRequest request) {
        String templateIdStr = request.pathVariable("templateId");

        try {
            Long templateId = Long.parseLong(templateIdStr);

            return ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(languageTemplateService.findAllByTemplateId(templateId), LanguageTemplate.class)
                    .onErrorResume(throwable -> {
                        System.err.println("Error retrieving language templates: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to retrieve language templates"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid template ID format"));
        }
    }

    public Mono<ServerResponse> getLanguageTemplateByTemplateIdAndLanguage(ServerRequest request) {
        String templateIdStr = request.pathVariable("templateId");
        String languageCode = request.pathVariable("language");

        try {
            Long templateId = Long.parseLong(templateIdStr);
            Language language = Language.fromCode(languageCode);

            return languageTemplateService.findByTemplateIdAndLanguage(templateId, language)
                    .flatMap(content -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(content)))
                    .switchIfEmpty(ServerResponse.notFound().build())
                    .onErrorResume(throwable -> {
                        System.err.println("Error retrieving language template: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to retrieve language template"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid template ID format"));
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid language code"));
        }
    }

    public Mono<ServerResponse> createLanguageTemplate(ServerRequest request) {
        return request.bodyToMono(LanguageTemplate.class)
                .flatMap(languageTemplate -> languageTemplateService.save(languageTemplate))
                .flatMap(content -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(content)))
                .onErrorResume(throwable -> {
                    System.err.println("Error creating language template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to create language template"));
                });
    }

    public Mono<ServerResponse> updateLanguageTemplate(ServerRequest request) {
        String idStr = request.pathVariable("id");

        try {
            Long id = Long.parseLong(idStr);

            return request.bodyToMono(LanguageTemplate.class)
                    .flatMap(languageTemplate -> languageTemplateService.update(id, languageTemplate))
                    .flatMap(content -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(content)))
                    .switchIfEmpty(ServerResponse.notFound().build())
                    .onErrorResume(throwable -> {
                        System.err.println("Error updating language template: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BodyInserters.fromValue("Failed to update language template"));
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid ID format"));
        }
    }

    public Mono<ServerResponse> deleteLanguageTemplate(ServerRequest request) {
        String idStr = request.pathVariable("id");

        try {
            Long id = Long.parseLong(idStr);

            return languageTemplateService.deleteById(id)
                    .then(ServerResponse.noContent().build())
                    .onErrorResume(throwable -> {
                        System.err.println("Error deleting language template: " + throwable.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .build();
                    });
        } catch (NumberFormatException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid ID format"));
        }
    }

    public Mono<ServerResponse> handle(ServerRequest request) {
        // 默认处理方法，可以根据需要实现
        return ServerResponse.badRequest().build();
    }
}
