package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.dto.template.FillTemplateRequest;
import com.github.waitlight.asskicker.dto.template.FillTemplateResponse;
import com.github.waitlight.asskicker.dto.template.TemplateDTO;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.service.TemplateService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TemplateHandler {

    private final TemplateService templateService;
    private final Validator validator;

    public Mono<ServerResponse> createTemplate(ServerRequest request) {
        return request.bodyToMono(TemplateDTO.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required")))
                .flatMap(this::validateDto)
                .map(this::toTemplateEntity)
                .flatMap(template -> templateService.createTemplate(template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse.status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString())))
                .onErrorResume(throwable -> {
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
                    System.err.println("Error retrieving template by code: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to retrieve template by code"));
                });
    }

    public Mono<ServerResponse> updateTemplate(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(TemplateDTO.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request body"))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required")))
                .flatMap(this::validateDto)
                .map(this::toTemplateEntity)
                .flatMap(template -> templateService.updateTemplate(id, template))
                .flatMap(template -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(template)))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse.status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString())))
                .onErrorResume(throwable -> {
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
                    System.err.println("Error deleting template: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build();
                });
    }

    public Mono<ServerResponse> getTemplateContentByLanguage(ServerRequest request) {
        String templateId = request.pathVariable("templateId");
        String languageCode = request.pathVariable("language");
        try {
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
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid language code"));
        }
    }

    public Mono<ServerResponse> saveTemplateContentByLanguage(ServerRequest request) {
        String templateId = request.pathVariable("templateId");
        String languageCode = request.pathVariable("language");
        try {
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
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .body(BodyInserters.fromValue("Invalid language code"));
        }
    }

    public Mono<ServerResponse> getAllTemplateContentsByTemplateId(ServerRequest request) {
        String templateId = request.pathVariable("templateId");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(templateService.getAllTemplateContentsByTemplateId(templateId), LanguageTemplateEntity.class)
                .onErrorResume(throwable -> {
                    System.err.println("Error retrieving all template contents: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to retrieve all template contents"));
                });
    }

    public Mono<ServerResponse> fillTemplate(ServerRequest request) {
        return request.bodyToMono(FillTemplateRequest.class)
                // TODO: replace with new template rendering implementation after migration.
                .map(req -> new FillTemplateResponse(null, "TODO: 新模板渲染逻辑尚未接入"))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(resp)))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode()).body(BodyInserters.fromValue(ex.getReason())));
    }

    public Mono<ServerResponse> listTemplates(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(templateService.findAll(0, Integer.MAX_VALUE), TemplateEntity.class)
                .onErrorResume(throwable -> {
                    System.err.println("Error listing templates: " + throwable.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BodyInserters.fromValue("Failed to list templates"));
                });
    }

    public Mono<ServerResponse> handle(ServerRequest request) {
        return ServerResponse.badRequest().build();
    }

    private Mono<TemplateDTO> validateDto(TemplateDTO dto) {
        Set<ConstraintViolation<TemplateDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
        }
        return Mono.just(dto);
    }

    private TemplateEntity toTemplateEntity(TemplateDTO input) {
        TemplateEntity entity = new TemplateEntity();
        if (input == null) {
            return entity;
        }
        entity.setName(input.getName());
        entity.setCode(input.getCode());
        entity.setDescription(input.getDescription());
        entity.setChannelType(input.getChannelType());
        if (input.getAttributes() != null) {
            entity.setAttributes(new LinkedHashMap<>(input.getAttributes()));
        } else {
            entity.setAttributes(null);
        }
        return entity;
    }
}
