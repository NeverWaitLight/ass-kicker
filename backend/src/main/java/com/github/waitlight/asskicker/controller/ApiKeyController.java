package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.apikey.ApiKeyView;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyRequest;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyResponse;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "ApiKeyController")
@RestController
@RequestMapping("/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<RespWrapper<CreateApiKeyResponse>> createApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.createApiKey(principal.userId(), request)
                .map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<RespWrapper<List<ApiKeyView>>> listApiKeys(@AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.listApiKeys(principal.userId())
                .map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revokeApiKey(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return apiKeyService.revokeApiKey(principal.userId(), id);
    }
}