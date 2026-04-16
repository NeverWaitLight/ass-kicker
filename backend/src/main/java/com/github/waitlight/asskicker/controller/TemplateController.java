package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import com.github.waitlight.asskicker.converter.TemplateConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.template.CreateTemplateDTO;
import com.github.waitlight.asskicker.dto.template.TemplateVO;
import com.github.waitlight.asskicker.dto.template.UpdateTemplateDTO;
import com.github.waitlight.asskicker.service.TemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "TemplateController")
@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Validated
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateConverter templateConverter;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<TemplateVO>> create(@Valid @RequestBody CreateTemplateDTO request) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(templateService::create)
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping
    public Mono<Resp<TemplateVO>> update(@Valid @RequestBody UpdateTemplateDTO request) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(patch -> templateService.update(request.getId(), patch))
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @NotBlank String id) {
        return templateService.delete(id);
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<TemplateVO>> getById(@PathVariable String id) {
        return templateService.findById(id)
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<TemplateVO>> page(@Validated PageReq pageReq) {
        int page = pageReq.getPage();
        int size = pageReq.getSize();
        String keyword = pageReq.getKeyword();
        int offset = (page - 1) * size;

        return templateService.count(keyword)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageResp.success(page, size, total, List.of()));
                    }
                    return templateService.list(keyword, size, offset)
                            .map(templateConverter::toVO)
                            .collectList()
                            .map(templates -> PageResp.success(page, size, total, templates));
                });
    }

    @Operation(summary = "getByCode", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/code/{code}")
    public Mono<Resp<TemplateVO>> getByCode(@PathVariable String code) {
        return templateService.findByCode(code)
                .map(templateConverter::toVO)
                .map(Resp::success);
    }
}
