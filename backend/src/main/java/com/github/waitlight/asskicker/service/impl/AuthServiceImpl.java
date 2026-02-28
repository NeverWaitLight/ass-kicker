package com.github.waitlight.asskicker.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.TokenResponse;
import com.github.waitlight.asskicker.model.User;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.security.JwtService;
import com.github.waitlight.asskicker.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserConverter userMapStructer;
    private final CaffeineCacheConfig caffeineCacheConfig;

    private AsyncLoadingCache<String, Optional<User>> userByUsernameCache;
    private AsyncLoadingCache<String, Optional<User>> userByIdCache;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, UserConverter userMapStructer,
            CaffeineCacheConfig caffeineCacheConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapStructer = userMapStructer;
        this.caffeineCacheConfig = caffeineCacheConfig;
    }

    @PostConstruct
    void initCaches() {
        userByUsernameCache = caffeineCacheConfig
                .buildCache((username, executor) -> userRepository.findByUsername(username)
                        .map(Optional::of)
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());

        userByIdCache = caffeineCacheConfig.buildCache((id, executor) -> userRepository.findById(id)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .toFuture());
    }

    @Override
    public Mono<TokenResponse> login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }
        String username = request.username().trim();
        return Mono.fromFuture(userByUsernameCache.get(username))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"))))
                .flatMap(user -> {
                    if (user.getStatus() == UserStatus.DISABLED) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "用户已被禁用"));
                    }
                    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
                    }
                    long now = Instant.now().toEpochMilli();
                    user.setLastLoginAt(now);
                    user.setUpdatedAt(now);
                    return userRepository.save(user)
                            .doOnSuccess(saved -> {
                                userByUsernameCache.synchronous().invalidate(username);
                                userByIdCache.synchronous().invalidate(saved.getId());
                            });
                })
                .map(user -> new TokenResponse(
                        jwtService.generateAccessToken(user),
                        jwtService.generateRefreshToken(user),
                        userMapStructer.toView(user)));
    }

    @Override
    public Mono<TokenResponse> refresh(String refreshToken) {
        if (isBlank(refreshToken)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "刷新令牌不能为空"));
        }
        return Mono.fromCallable(() -> jwtService.parseRefreshToken(refreshToken))
                .onErrorMap(ex -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效"))
                .flatMap(payload -> Mono.fromFuture(userByIdCache.get(payload.userId()))
                        .flatMap(opt -> opt
                                .map(Mono::just)
                                .orElseGet(() -> Mono
                                        .error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"))))
                        .flatMap(user -> {
                            if (user.getStatus() == UserStatus.DISABLED) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "用户已被禁用"));
                            }
                            return Mono.just(new TokenResponse(
                                    jwtService.generateAccessToken(user),
                                    jwtService.generateRefreshToken(user),
                                    userMapStructer.toView(user)));
                        }));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
