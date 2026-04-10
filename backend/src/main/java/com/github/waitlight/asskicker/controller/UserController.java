package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.PageRespWrapper;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.user.CreateUserRequest;
import com.github.waitlight.asskicker.dto.user.ResetPasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdatePasswordRequest;
import com.github.waitlight.asskicker.dto.user.UpdateUsernameRequest;
import com.github.waitlight.asskicker.dto.user.UserView;
import com.github.waitlight.asskicker.security.UserPrincipal;
import com.github.waitlight.asskicker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<RespWrapper<UserView>> createUser(@RequestBody CreateUserRequest request) {
        return userService.createUser(request).map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageRespWrapper<UserView>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return userService.listUsers(page, size, keyword)
                .map(pr -> PageRespWrapper.success(pr.page(), pr.size(), pr.total(), pr.data()));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<RespWrapper<UserView>> getUserById(@PathVariable String id) {
        return userService.getUserById(id).map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteUser(@PathVariable String id) {
        return userService.deleteUser(id);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}/password")
    public Mono<RespWrapper<UserView>> resetPassword(@PathVariable String id,
            @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(id, request.newPassword()).map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping("/me")
    public Mono<RespWrapper<UserView>> updateMeUsername(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateUsernameRequest request) {
        return userService.updateUsername(principal.userId(), request).map(RespWrapper::success);
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/me/password")
    public Mono<RespWrapper<UserView>> updateMePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdatePasswordRequest request) {
        return userService.updatePassword(principal.userId(), request).map(RespWrapper::success);
    }
}
