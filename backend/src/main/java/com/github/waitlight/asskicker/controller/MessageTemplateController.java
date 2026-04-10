package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "MessageTemplateController")
@RestController
@RequestMapping("/v1/message-templates")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;
    private final MessageTemplateConverter messageTemplateConverter;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Resp<MessageTemplateDTO>> create(@Valid @RequestBody MessageTemplateDTO request) {
        MessageTemplateEntity entity = messageTemplateConverter.toEntity(request);
        return messageTemplateService.create(entity)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<MessageTemplateDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return messageTemplateService.page(page, size)
                .map(pr -> PageResp.success(pr.page(), pr.size(), pr.total(), pr.data()));
    }

    @Operation(summary = "getByCode", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/code/{code}")
    public Mono<Resp<MessageTemplateDTO>> getByCode(@PathVariable String code) {
        return messageTemplateService.findByCode(code)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<MessageTemplateDTO>> getById(@PathVariable String id) {
        return messageTemplateService.findById(id)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}")
    public Mono<Resp<MessageTemplateDTO>> update(
            @PathVariable String id,
            @Valid @RequestBody MessageTemplateDTO request) {
        MessageTemplateEntity entity = messageTemplateConverter.toEntity(request);
        return messageTemplateService.update(id, entity)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return messageTemplateService.delete(id);
    }
}