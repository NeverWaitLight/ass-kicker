package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.apikey.ApiKeyVO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyDTO;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyVO;
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

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<CreateApiKeyVO>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateApiKeyDTO request) {
        return apiKeyService.create(principal.userId(), request)
                .map(Resp::success);
    }

    @Operation(summary = "list", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<Resp<List<ApiKeyVO>>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.list(principal.userId())
                .map(Resp::success);
    }

    @Operation(summary = "revoke", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revoke(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String id) {
        return apiKeyService.revoke(principal.userId(), id);
    }
}