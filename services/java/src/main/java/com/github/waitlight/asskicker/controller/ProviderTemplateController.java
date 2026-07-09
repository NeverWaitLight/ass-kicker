package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.github.waitlight.asskicker.dto.template.CreateProviderTemplateDTO;
import com.github.waitlight.asskicker.dto.template.ProviderTemplateVO;
import com.github.waitlight.asskicker.dto.template.UpdateProviderTemplateDTO;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ProviderTemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "服务商模板")
@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
@Validated
public class ProviderTemplateController {

    private final ProviderTemplateService providerTemplateService;
    private final TemplateConverter templateConverter;

    @Operation(summary = "创建服务商模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping("/provider")
    public Mono<Resp<ProviderTemplateVO>> createProvider(@Valid @RequestBody CreateProviderTemplateDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(entity -> providerTemplateService.createProvider(entity, principal.userId()))
                .map(templateConverter::toProviderVO)
                .map(Resp::success);
    }

    @Operation(summary = "更新服务商模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/provider/{id}")
    public Mono<Resp<ProviderTemplateVO>> updateProvider(@PathVariable @NotBlank String id,
            @Valid @RequestBody UpdateProviderTemplateDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Mono.just(request)
                .map(templateConverter::toEntity)
                .flatMap(patch -> providerTemplateService.updateProvider(id, patch, principal.userId()))
                .map(templateConverter::toProviderVO)
                .map(Resp::success);
    }

    @Operation(summary = "删除服务商模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/provider/{id}")
    public Mono<Resp<Void>> deleteProvider(@PathVariable @NotBlank String id) {
        return providerTemplateService.deleteProviderById(id)
                .thenReturn(Resp.success(null));
    }

    @Operation(summary = "查询服务商模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/provider/{id}")
    public Mono<Resp<ProviderTemplateVO>> getProviderById(@PathVariable @NotBlank String id) {
        return providerTemplateService.findProviderById(id)
                .map(templateConverter::toProviderVO)
                .map(Resp::success);
    }

    @Operation(summary = "按语言模板与服务商查询", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/localized/{localizedTemplateId}/provider/{provider}")
    public Mono<Resp<ProviderTemplateVO>> getProviderByLocalizedAndProvider(
            @PathVariable @NotBlank String localizedTemplateId,
            @PathVariable @NotNull ChannelProvider provider) {
        return providerTemplateService.findProvider(localizedTemplateId, provider)
                .map(templateConverter::toProviderVO)
                .map(Resp::success);
    }

    @Operation(summary = "查询语言模板下的所有服务商模板", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/localized/{localizedTemplateId}/provider")
    public Mono<Resp<List<ProviderTemplateVO>>> listProvider(@PathVariable @NotBlank String localizedTemplateId) {
        return providerTemplateService.listProvider(localizedTemplateId)
                .map(templateConverter::toProviderVO)
                .collectList()
                .map(Resp::success);
    }
}
