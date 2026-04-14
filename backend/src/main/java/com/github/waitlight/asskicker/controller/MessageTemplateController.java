package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.converter.MessageTemplateConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.template.MessageTemplateDTO;
import com.github.waitlight.asskicker.model.TemplateEntity;
import com.github.waitlight.asskicker.service.MessageTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "MessageTemplateController")
@RestController
@RequestMapping("/v1/message-templates")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;
    private final MessageTemplateConverter messageTemplateConverter;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<MessageTemplateDTO>> create(@RequestBody @Validated MessageTemplateDTO request) {
        TemplateEntity entity = messageTemplateConverter.toEntity(request);
        return messageTemplateService.create(entity)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping
    public Mono<Resp<MessageTemplateDTO>> update(@RequestBody @Validated MessageTemplateDTO request) {
        TemplateEntity entity = messageTemplateConverter.toEntity(request);
        return messageTemplateService.update(request.getId(), entity)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @NotBlank String id) {
        return messageTemplateService.delete(id);
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<MessageTemplateDTO>> getById(@PathVariable String id) {
        return messageTemplateService.findById(id)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<MessageTemplateDTO>> page(@Validated PageReq pageReq) {
        int page = pageReq.getPage();
        int size = pageReq.getSize();
        String keyword = pageReq.getKeyword();
        int offset = (page - 1) * size;

        return messageTemplateService.count(keyword)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageResp.success(page, size, total, List.of()));
                    }
                    return messageTemplateService.list(keyword, size, offset)
                            .map(messageTemplateConverter::toDto)
                            .collectList()
                            .map(templates -> PageResp.success(page, size, total, templates));
                });
    }

    @Operation(summary = "getByCode", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/code/{code}")
    public Mono<Resp<MessageTemplateDTO>> getByCode(@PathVariable String code) {
        return messageTemplateService.findByCode(code)
                .map(messageTemplateConverter::toDto)
                .map(Resp::success);
    }
}