package com.github.waitlight.asskicker.service.impl;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.TokenResponse;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.repository.UserRepository;
import com.github.waitlight.asskicker.security.JwtService;
import com.github.waitlight.asskicker.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserConverter userMapStructer;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, UserConverter userMapStructer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapStructer = userMapStructer;
    }

    @Override
    public Mono<TokenResponse> login(LoginRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名或密码不能为空"));
        }
        String username = request.username().trim();
        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误")))
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
                    return userRepository.save(user);
                })
                .map(user -> new TokenResponse(
                        jwtService.generateAccessToken(user),
                        jwtService.generateRefreshToken(user),
                        userMapStructer.toView(user)
                ));
    }

    @Override
    public Mono<TokenResponse> refresh(String refreshToken) {
        if (isBlank(refreshToken)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "刷新令牌不能为空"));
        }
        return Mono.fromCallable(() -> jwtService.parseRefreshToken(refreshToken))
                .onErrorMap(ex -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效"))
                .flatMap(payload -> userRepository.findById(payload.userId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在")))
                        .flatMap(user -> {
                            if (user.getStatus() == UserStatus.DISABLED) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "用户已被禁用"));
                            }
                            return Mono.just(new TokenResponse(
                                    jwtService.generateAccessToken(user),
                                    jwtService.generateRefreshToken(user),
                                    userMapStructer.toView(user)
                            ));
                        }));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
