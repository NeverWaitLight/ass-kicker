package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.apikey.ApiKeyView;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyRequest;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiKeyService {

    Mono<CreateApiKeyResponse> createApiKey(String userId, CreateApiKeyRequest request);

    Flux<ApiKeyView> listApiKeys(String userId);

    Mono<Void> revokeApiKey(String userId, String id);
}
