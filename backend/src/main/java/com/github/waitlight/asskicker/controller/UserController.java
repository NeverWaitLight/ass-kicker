package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.PageRespWrapper;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.user.ResetPasswordDTO;
import com.github.waitlight.asskicker.dto.user.UpdatePasswordDTO;
import com.github.waitlight.asskicker.dto.user.UpdateUsernameDTO;
import com.github.waitlight.asskicker.dto.user.UserDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.UserService;
import com.github.waitlight.asskicker.validation.Create;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "UserController")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<RespWrapper<UserVO>> create(@RequestBody @Validated(Create.class) UserDTO user) {
        return userService.create(userConverter.toEntity(user)).map(RespWrapper::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageRespWrapper<UserVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return userService.page(page, size, keyword)
                .map(pr -> PageRespWrapper.success(pr.page(), pr.size(), pr.total(), pr.data()));
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<RespWrapper<UserVO>> getById(@PathVariable String id) {
        return userService.getById(id).map(RespWrapper::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return userService.delete(id);
    }

    @Operation(summary = "resetPassword", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}/password")
    public Mono<RespWrapper<UserVO>> resetPassword(@PathVariable String id,
            @RequestBody ResetPasswordDTO request) {
        return userService.resetPassword(id, request.newPassword()).map(RespWrapper::success);
    }

    @Operation(summary = "updateUsername", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping("/me")
    public Mono<RespWrapper<UserVO>> updateUsername(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateUsernameDTO request) {
        return userService.updateUsername(principal.userId(), request).map(RespWrapper::success);
    }

    @Operation(summary = "updatePassword", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/me/password")
    public Mono<RespWrapper<UserVO>> updatePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdatePasswordDTO request) {
        return userService.updatePassword(principal.userId(), request).map(RespWrapper::success);
    }
}
