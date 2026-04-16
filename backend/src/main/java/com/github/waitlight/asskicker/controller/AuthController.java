package com.github.waitlight.asskicker.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.auth.LoginDTO;
import com.github.waitlight.asskicker.dto.auth.RefreshDTO;
import com.github.waitlight.asskicker.dto.auth.TokenVO;
import com.github.waitlight.asskicker.dto.user.CreateUserDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.service.AuthService;
import com.github.waitlight.asskicker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "AuthController")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final UserService userService;
        private final UserConverter userConverter;

        @Operation(summary = "login")
        @PostMapping("/login")
        public Mono<Resp<TokenVO>> login(@RequestBody @Validated LoginDTO request) {
                request = new LoginDTO(request.username().trim(), request.password());
                return authService.login(request)
                                .map(Resp::success);
        }

        @Operation(summary = "register")
        @PostMapping("/register")
        public Mono<Resp<UserVO>> register(@RequestBody @Validated CreateUserDTO request) {
                request = new CreateUserDTO(request.username().trim(), request.password());
                UserEntity entity = userConverter.toEntity(request);
                return userService.count(null)
                                .flatMap(count -> {
                                        if (count == 0) {
                                                entity.setRole(UserRole.ADMIN);
                                        }
                                        return userService.create(entity);
                                })
                                .map(userConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "refresh")
        @PostMapping("/refresh")
        public Mono<Resp<TokenVO>> refresh(@RequestBody @Validated RefreshDTO request) {
                return authService.refresh(request.refreshToken())
                                .map(Resp::success);
        }
}
