package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.waitlight.asskicker.model.ApiKeyStatus;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.repository.ApiKeyRepository;
import com.github.waitlight.asskicker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    private final Cache<String, CachedAuthResult> authCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<UserPrincipal> authenticate(String rawKey) {
        if (rawKey == null || rawKey.length() < 12) {
            return Mono.error(new BadCredentialsException("Invalid API Key"));
        }

        String cacheKey = sha256(rawKey);
        CachedAuthResult cached = authCache.getIfPresent(cacheKey);
        if (cached != null) {
            if (cached.expiresAt() != null && cached.expiresAt() < Instant.now().toEpochMilli()) {
                authCache.invalidate(cacheKey);
                return Mono.error(new BadCredentialsException("API Key expired"));
            }
            return Mono.just(cached.principal());
        }

        String keyPrefix = rawKey.substring(0, 12);
        return apiKeyRepository.findByKeyPrefix(keyPrefix)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid API Key")))
                .flatMap(apiKey -> {
                    if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
                        return Mono.error(new BadCredentialsException("API Key revoked"));
                    }
                    if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt() < Instant.now().toEpochMilli()) {
                        return Mono.error(new BadCredentialsException("API Key expired"));
                    }
                    return Mono.fromCallable(() -> passwordEncoder.matches(rawKey, apiKey.getKeyHash()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(matches -> {
                                if (!matches) {
                                    return Mono.error(new BadCredentialsException("Invalid API Key"));
                                }
                                UserPrincipal principal = new UserPrincipal(apiKey.getUserId(), UserRole.USER);
                                authCache.put(cacheKey, new CachedAuthResult(principal, apiKey.getExpiresAt()));
                                return Mono.just(principal);
                            });
                });
    }

    public void invalidateCache(String keyPrefix) {
        authCache.asMap().keySet().removeIf(k -> true);
    }

    record CachedAuthResult(UserPrincipal principal, Long expiresAt) {
    }
}
