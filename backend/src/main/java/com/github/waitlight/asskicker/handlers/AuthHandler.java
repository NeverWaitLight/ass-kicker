package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.RefreshRequest;
import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.AuthService;
import com.github.waitlight.asskicker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class AuthHandler {

    private final AuthService authService;
    private final UserService userService;

    public AuthHandler(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(authService::login)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "登录失败" : ex.getReason()));
    }

    public Mono<ServerResponse> refresh(ServerRequest request) {
        return request.bodyToMono(RefreshRequest.class)
                .flatMap(body -> authService.refresh(body.refreshToken()))
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "刷新失败" : ex.getReason()));
    }

    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(RegisterRequest.class)
                .flatMap(userService::registerUser)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "注册失败" : ex.getReason()));
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .flatMap(principal -> userService.getUserById(principal.userId()))
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build())
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取用户失败" : ex.getReason()));
    }
}
