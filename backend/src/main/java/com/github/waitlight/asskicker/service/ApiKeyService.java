package com.github.waitlight.asskicker.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.waitlight.asskicker.dto.apikey.ExpiresIn;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.model.ApiKeyEntity;
import com.github.waitlight.asskicker.model.ApiKeyStatus;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String API_KEY_PREFIX = "ak_";

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyAuthService apiKeyAuthService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public record CreateResult(ApiKeyEntity entity, String rawKey) {
    }

    public Mono<CreateResult> create(String userId, String name, ExpiresIn expiresIn) {
        String raw = API_KEY_PREFIX + snowflakeIdGenerator.nextIdString();
        String keyPrefix = raw.substring(0, 12);
        String keyHash = passwordEncoder.encode(raw);
        long now = Instant.now().toEpochMilli();

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(snowflakeIdGenerator.nextIdString());
        apiKey.setUserId(userId);
        apiKey.setName(name);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setExpiresAt(expiresIn.calculateExpiresAt());
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setCreatedAt(now);

        return apiKeyRepository.save(apiKey)
                .map(saved -> new CreateResult(saved, raw));
    }

    public Mono<ApiKeyEntity> findById(String userId, String id) {
        return apiKeyRepository.findById(id)
                .filter(apiKey -> userId.equals(apiKey.getUserId()));
    }

    public Flux<ApiKeyEntity> list(String userId) {
        return apiKeyRepository.findByUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
    }

    public Mono<Void> revoke(String userId, String id) {
        return apiKeyRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("apikey.id.notFound", id)))
                .flatMap(apiKey -> {
                    if (!userId.equals(apiKey.getUserId())) {
                        return Mono.error(new PermissionDeniedException("apikey.permission.denied"));
                    }
                    if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
                        return Mono.error(new BadRequestException("apikey.revoked"));
                    }
                    apiKey.setStatus(ApiKeyStatus.REVOKED);
                    apiKey.setRevokedAt(Instant.now().toEpochMilli());
                    return apiKeyRepository.save(apiKey)
                            .doOnSuccess(saved -> apiKeyAuthService.invalidateCache(saved.getKeyPrefix()));
                })
                .then();
    }

}
