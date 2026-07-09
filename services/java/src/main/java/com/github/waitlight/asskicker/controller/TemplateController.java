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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.TemplateConverter;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.template.CreateTemplateDTO;
import com.github.waitlight.asskicker.dto.template.TemplateVO;
import com.github.waitlight.asskicker.dto.template.UpdateTemplateDTO;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.TemplateService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "消息模板")
@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Validated
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateConverter templateConverter;

    @Operation(summary = "创建模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<TemplateVO>> create(@Valid @RequestBody CreateTemplateDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(entity -> templateService.create(entity, principal.userId()))
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "更新模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}")
    public Mono<Resp<TemplateVO>> update(@PathVariable @NotBlank String id,
            @Valid @RequestBody UpdateTemplateDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(patch -> templateService.update(id, patch, principal.userId()))
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "删除模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    public Mono<Resp<Void>> delete(@PathVariable @NotBlank String id) {
        return templateService.delete(id)
                .thenReturn(Resp.success(null));
    }

    @Operation(summary = "查询模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<TemplateVO>> getById(@PathVariable @NotBlank String id) {
        return templateService.findById(id)
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "分页查询", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<TemplateVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ChannelType channelType) {
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        int offset = (p - 1) * s;

        return templateService.count(keyword, channelType)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageResp.success(p, s, total, List.of()));
                    }
                    return templateService.list(keyword, channelType, s, offset)
                            .map(templateConverter::toVO)
                            .collectList()
                            .map(templates -> PageResp.success(p, s, total, templates));
                });
    }

    @Operation(summary = "按编码查询", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/code/{code}")
    public Mono<Resp<TemplateVO>> getByCode(@PathVariable @NotBlank String code) {
        return templateService.findByCode(code)
                .map(templateConverter::toVO)
                .map(Resp::success);
    }

}
