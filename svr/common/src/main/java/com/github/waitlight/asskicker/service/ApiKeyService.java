package com.github.waitlight.asskicker.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.cache.CaffeineCacheConfig;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.exception.UnauthorizedException;
import com.github.waitlight.asskicker.model.ApiKeyEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String API_KEY_PREFIX = "ak_";

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, UserPrincipal> authCache;

    @PostConstruct
    public void init() {
        authCache = caffeineCacheConfig.buildCache((rawKey, executor) ->
                apiKeyRepository.findByKeyPrefix(rawKey.substring(0, 12))
                        .switchIfEmpty(Mono.error(new UnauthorizedException("apikey.invalid")))
                        .flatMap(apiKey -> Mono.fromCallable(() -> passwordEncoder.matches(rawKey, apiKey.getKeyHash()))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(matches -> {
                                    if (!matches) {
                                        return Mono.error(new UnauthorizedException("apikey.invalid"));
                                    }
                                    return Mono.just(new UserPrincipal(apiKey.getUserId(), UserRole.MEMBER));
                                }))
                        .toFuture());
    }

    public record CreateResult(ApiKeyEntity entity, String rawKey) {
    }

    public Mono<UserPrincipal> authenticate(String rawKey) {
        if (rawKey == null || rawKey.length() < 12) {
            return Mono.error(new UnauthorizedException("apikey.invalid"));
        }
        return Mono.fromCompletionStage(authCache.get(rawKey));
    }

    public Mono<CreateResult> create(String userId, String name) {
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
        apiKey.setCreatedAt(now);

        return apiKeyRepository.save(apiKey)
                .map(saved -> new CreateResult(saved, raw));
    }

    public Flux<ApiKeyEntity> list(String userId) {
        return apiKeyRepository.findByUserId(userId);
    }

    public Mono<Void> delete(String userId, String id) {
        return apiKeyRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("apikey.id.notFound", id)))
                .flatMap(apiKey -> {
                    if (!userId.equals(apiKey.getUserId())) {
                        return Mono.error(new PermissionDeniedException("apikey.permission.denied"));
                    }
                    return apiKeyRepository.deleteById(id)
                            .doOnSuccess(v -> authCache.synchronous().invalidateAll());
                });
    }

    public Mono<ApiKeyEntity> update(String userId, String id, String name) {
        return apiKeyRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("apikey.id.notFound", id)))
                .flatMap(apiKey -> {
                    if (!userId.equals(apiKey.getUserId())) {
                        return Mono.error(new PermissionDeniedException("apikey.permission.denied"));
                    }
                    apiKey.setName(name);
                    return apiKeyRepository.save(apiKey);
                });
    }
}