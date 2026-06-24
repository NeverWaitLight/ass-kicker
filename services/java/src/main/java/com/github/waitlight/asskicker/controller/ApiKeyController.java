package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.ApiKeyConverter;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.apikey.ApiKeyVO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyDTO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyVO;
import com.github.waitlight.asskicker.dto.apikey.UpdateApiKeyDTO;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ApiKeyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "API Keys")
@RestController
@RequestMapping("/v1/apikeys")
@RequiredArgsConstructor
@Validated
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ApiKeyConverter apiKeyConverter;

    @Operation(summary = "创建密钥", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<CreateApiKeyVO>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated CreateApiKeyDTO req) {
        req = new CreateApiKeyDTO(req.name().trim());
        return apiKeyService.create(principal.userId(), req.name())
                .map(result -> apiKeyConverter.toCreateVO(result.entity(), result.rawKey()))
                .map(Resp::success);
    }

    @Operation(summary = "密钥列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<Resp<List<ApiKeyVO>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.list(principal.userId())
                .map(apiKeyConverter::toVO)
                .collectList()
                .map(Resp::success);
    }

    @Operation(summary = "更新密钥", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping
    public Mono<Resp<ApiKeyVO>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UpdateApiKeyDTO req) {
        return apiKeyService.update(principal.userId(), req.id(), req.name().trim())
                .map(apiKeyConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "删除密钥", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @NotBlank String id) {
        return apiKeyService.delete(principal.userId(), id);
    }
}
