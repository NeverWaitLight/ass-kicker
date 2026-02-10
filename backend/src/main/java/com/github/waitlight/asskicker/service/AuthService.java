package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.TokenResponse;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<TokenResponse> login(LoginRequest request);

    Mono<TokenResponse> refresh(String refreshToken);
}
