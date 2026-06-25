package com.github.waitlight.asskicker.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.auth.RefreshDTO;
import com.github.waitlight.asskicker.dto.auth.TokenVO;
import com.github.waitlight.asskicker.dto.auth.SignInDTO;
import com.github.waitlight.asskicker.dto.auth.SignUpDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.model.UserEntity;
import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.service.AuthService;
import com.github.waitlight.asskicker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "登录授权")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final UserService userService;
        private final UserConverter userConverter;

        @Operation(summary = "登录")
        @PostMapping("/signin")
        public Mono<Resp<TokenVO>> signIn(@RequestBody @Validated SignInDTO dto) {
                dto = new SignInDTO(dto.username().trim(), dto.password());
                return authService.signIn(dto)
                                .map(Resp::success);
        }

        @Operation(summary = "注册")
        @PostMapping("/signup")
        public Mono<Resp<UserVO>> signUp(@RequestBody @Validated SignUpDTO dto) {
                dto = new SignUpDTO(dto.username().trim(), dto.password());
                UserEntity entity = userConverter.toEntity(dto);
                entity.setRole(UserRole.DEVELOPER);
                return userService.create(entity)
                                .map(userConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "刷新令牌")
        @PostMapping("/refresh")
        public Mono<Resp<TokenVO>> refresh(@RequestBody @Validated RefreshDTO request) {
                return authService.refresh(request.refreshToken())
                                .map(Resp::success);
        }
}
