package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.apikey.ApiKeyView;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyRequest;
import com.github.waitlight.asskicker.dto.apikey.CreateApiKeyResponse;
import com.github.waitlight.asskicker.model.ApiKeyEntity;
import com.github.waitlight.asskicker.model.ApiKeyStatus;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyAuthService apiKeyAuthService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public Mono<CreateApiKeyResponse> create(String userId, CreateApiKeyRequest request) {
        String raw = "ak_" + snowflakeIdGenerator.nextIdString();
        String keyPrefix = raw.substring(0, 12);
        String keyHash = passwordEncoder.encode(raw);
        long now = Instant.now().toEpochMilli();

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(snowflakeIdGenerator.nextIdString());
        apiKey.setUserId(userId);
        apiKey.setName(request.name());
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setExpiresAt(request.expiresAt());
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setCreatedAt(now);

        return apiKeyRepository.save(apiKey)
                .map(saved -> new CreateApiKeyResponse(
                        saved.getId(),
                        saved.getName(),
                        saved.getKeyPrefix(),
                        raw,
                        saved.getExpiresAt(),
                        saved.getStatus(),
                        saved.getCreatedAt()
                ));
    }

    public Mono<List<ApiKeyView>> list(String userId) {
        return apiKeyRepository.findByUserIdAndStatus(userId, ApiKeyStatus.ACTIVE)
                .map(apiKey -> new ApiKeyView(
                        apiKey.getId(),
                        apiKey.getName(),
                        apiKey.getKeyPrefix(),
                        apiKey.getExpiresAt(),
                        apiKey.getStatus(),
                        apiKey.getCreatedAt(),
                        apiKey.getRevokedAt()
                ))
                .collectList();
    }

    public Mono<Void> revoke(String userId, String id) {
        return apiKeyRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "API Key 不存在")))
                .flatMap(apiKey -> {
                    if (!userId.equals(apiKey.getUserId())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "无权操作"));
                    }
                    if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "API Key 已销毁"));
                    }
                    apiKey.setStatus(ApiKeyStatus.REVOKED);
                    apiKey.setRevokedAt(Instant.now().toEpochMilli());
                    return apiKeyRepository.save(apiKey)
                            .doOnSuccess(saved -> apiKeyAuthService.invalidateCache(saved.getKeyPrefix()));
                })
                .then();
    }

}
