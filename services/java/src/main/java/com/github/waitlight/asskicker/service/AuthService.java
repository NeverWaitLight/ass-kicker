package com.github.waitlight.asskicker.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.auth.LoginDTO;
import com.github.waitlight.asskicker.dto.auth.TokenVO;
import com.github.waitlight.asskicker.exception.BadRequestException;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.exception.PermissionDeniedException;
import com.github.waitlight.asskicker.exception.UnauthorizedException;
import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.security.JwtService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserConverter userConverter;

    public Mono<TokenVO> login(LoginDTO request) {
        if (request == null || StringUtils.isBlank(request.username()) || StringUtils.isBlank(request.password())) {
            return Mono.error(new BadRequestException("auth.credentials.empty"));
        }
        return userService.findByUsername(request.username())
                .switchIfEmpty(Mono.error(new UnauthorizedException("auth.login.failed")))
                .flatMap(user -> {
                    if (user.getStatus() == UserStatus.DISABLED) {
                        return Mono.error(new PermissionDeniedException("auth.user.disabled"));
                    }
                    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                        return Mono.error(new UnauthorizedException("auth.login.failed"));
                    }
                    return userService.recordLogin(user);
                })
                .map(user -> new TokenVO(
                        jwtService.generateAccessToken(user),
                        jwtService.generateRefreshToken(user),
                        userConverter.toVO(user)));
    }

    public Mono<TokenVO> refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return Mono.error(new BadRequestException("auth.refreshToken.empty"));
        }
        return Mono.fromCallable(() -> jwtService.parseRefreshToken(refreshToken))
                .onErrorMap(ex -> new UnauthorizedException("auth.refreshToken.invalid"))
                .flatMap(payload -> userService.getById(payload.userId())
                        .onErrorResume(NotFoundException.class,
                                e -> Mono.error(new UnauthorizedException("auth.user.notFound")))
                        .flatMap(user -> {
                            if (user.getStatus() == UserStatus.DISABLED) {
                                return Mono.error(new PermissionDeniedException("auth.user.disabled"));
                            }
                            return Mono.just(new TokenVO(
                                    jwtService.generateAccessToken(user),
                                    jwtService.generateRefreshToken(user),
                                    userConverter.toVO(user)));
                        }));
    }
}
