package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.TemplateConverter;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.template.CreateLocalizedTemplateDTO;
import com.github.waitlight.asskicker.dto.template.LocalizedTemplateVO;
import com.github.waitlight.asskicker.dto.template.UpdateLocalizedTemplateDTO;
import com.github.waitlight.asskicker.service.LocalizedTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "语言模板")
@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Validated
public class LocalizedTemplateController {

    private final LocalizedTemplateService localizedTemplateService;
    private final TemplateConverter templateConverter;

    @Operation(summary = "创建语言模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping("/localized")
    public Mono<Resp<LocalizedTemplateVO>> createLocalized(@Valid @RequestBody CreateLocalizedTemplateDTO request) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(localizedTemplateService::createLocalized)
                .map(templateConverter::toLocalizedVO)
                .map(Resp::success);
    }

    @Operation(summary = "更新语言模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/localized/{id}")
    public Mono<Resp<LocalizedTemplateVO>> updateLocalized(@PathVariable @NotBlank String id,
            @Valid @RequestBody UpdateLocalizedTemplateDTO request) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(patch -> localizedTemplateService.updateLocalized(id, patch))
                .map(templateConverter::toLocalizedVO)
                .map(Resp::success);
    }

    @Operation(summary = "删除语言模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/localized/{id}")
    public Mono<Resp<Void>> deleteLocalized(@PathVariable @NotBlank String id) {
        return localizedTemplateService.deleteLocalizedById(id)
                .thenReturn(Resp.success(null));
    }

    @Operation(summary = "查询语言模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/localized/{id}")
    public Mono<Resp<LocalizedTemplateVO>> getLocalizedById(@PathVariable @NotBlank String id) {
        return localizedTemplateService.findLocalizedById(id)
                .map(templateConverter::toLocalizedVO)
                .map(Resp::success);
    }

    @Operation(summary = "查询模板下的所有语言模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{templateId}/localized")
    public Mono<Resp<List<LocalizedTemplateVO>>> listLocalized(@PathVariable @NotBlank String templateId) {
        return localizedTemplateService.listLocalized(templateId)
                .map(templateConverter::toLocalizedVO)
                .collectList()
                .map(Resp::success);
    }
}