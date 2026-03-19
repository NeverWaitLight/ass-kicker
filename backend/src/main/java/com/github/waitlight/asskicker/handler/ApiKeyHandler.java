package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.dto.apikey.ApiKeyView;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyRequest;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiKeyHandler {

    private final ApiKeyService apiKeyService;

    public Mono<ServerResponse> createApiKey(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(auth -> (UserPrincipal) auth.getPrincipal())
                .flatMap(principal -> request.bodyToMono(CreateApiKeyRequest.class)
                        .defaultIfEmpty(new CreateApiKeyRequest(null, null))
                        .flatMap(body -> apiKeyService.createApiKey(principal.userId(), body)))
                .flatMap(response -> ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "创建 API Key 失败" : ex.getReason()));
    }

    public Mono<ServerResponse> listApiKeys(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(auth -> (UserPrincipal) auth.getPrincipal())
                .flatMap(principal -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(apiKeyService.listApiKeys(principal.userId()), ApiKeyView.class))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取 API Key 列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> revokeApiKey(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.principal()
                .cast(Authentication.class)
                .map(auth -> (UserPrincipal) auth.getPrincipal())
                .flatMap(principal -> apiKeyService.revokeApiKey(principal.userId(), id))
                .then(ServerResponse.noContent().build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "销毁 API Key 失败" : ex.getReason()));
    }
}
