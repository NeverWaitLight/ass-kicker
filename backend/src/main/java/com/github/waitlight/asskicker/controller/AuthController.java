package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.auth.LoginRequest;
import com.github.waitlight.asskicker.dto.auth.RefreshRequest;
import com.github.waitlight.asskicker.dto.auth.RegisterRequest;
import com.github.waitlight.asskicker.dto.auth.TokenResponse;
import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.AuthService;
import com.github.waitlight.asskicker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Tag(name = "AuthController")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "login")
    @PostMapping("/login")
    public Mono<RespWrapper<TokenResponse>> login(@RequestBody LoginRequest request) {
        return authService.login(request)
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.error(new ResponseStatusException(ex.getStatusCode(),
                                ex.getReason() == null ? "登录失败" : ex.getReason())));
    }

    @Operation(summary = "register")
    @PostMapping("/register")
    public Mono<RespWrapper<UserView>> register(@RequestBody RegisterRequest request) {
        return userService.register(request)
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.error(new ResponseStatusException(ex.getStatusCode(),
                                ex.getReason() == null ? "注册失败" : ex.getReason())));
    }

    @Operation(summary = "refresh")
    @PostMapping("/refresh")
    public Mono<RespWrapper<TokenResponse>> refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken())
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.error(new ResponseStatusException(ex.getStatusCode(),
                                ex.getReason() == null ? "刷新失败" : ex.getReason())));
    }

    @Operation(summary = "me", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/me")
    public Mono<RespWrapper<UserView>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未认证"));
        }
        return userService.getById(principal.userId())
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.error(new ResponseStatusException(ex.getStatusCode(),
                                ex.getReason() == null ? "获取用户失败" : ex.getReason())));
    }
}