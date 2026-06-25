package com.github.waitlight.asskicker.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.user.UpdateMeDTO;
import com.github.waitlight.asskicker.dto.user.UpdateMyPasswordDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "个人信息")
@RestController
@RequestMapping("/v1/me")
@RequiredArgsConstructor
@Validated
public class MeController {

    private final UserService userService;
    private final UserConverter userConverter;

    @Operation(summary = "获取当前用户信息", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<Resp<UserVO>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getById(principal.userId())
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "修改当前用户信息", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping
    public Mono<Resp<UserVO>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UpdateMeDTO req) {
        return userService.updateUsername(principal.userId(), req.username())
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "修改当前用户密码", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/password")
    public Mono<Resp<UserVO>> updateMyPassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Validated UpdateMyPasswordDTO req) {
        return userService.resetPassword(principal.userId(), req.newPassword(), req.currPassword())
                .map(userConverter::toVO)
                .map(Resp::success);
    }
}
