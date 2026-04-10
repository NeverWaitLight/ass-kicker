package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.PageRespWrapper;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.user.UserDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.service.UserService;
import com.github.waitlight.asskicker.validation.Create;
import com.github.waitlight.asskicker.validation.ResetPassword;
import com.github.waitlight.asskicker.validation.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Tag(name = "UserController")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
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
        int offset = (page - 1) * size;

        return userService.count(keyword)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageRespWrapper.success(page, size, total, List.of()));
                    }
                    return userService.list(keyword, size, offset)
                            .map(userConverter::toView)
                            .collectList()
                            .map(users -> PageRespWrapper.success(page, size, total, users));
                });
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<RespWrapper<UserVO>> getById(@PathVariable String id) {
        return userService.getById(id).map(RespWrapper::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @NotBlank String id) {
        return userService.delete(id);
    }

    @Operation(summary = "resetPassword", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/password")
    public Mono<RespWrapper<UserVO>> resetPassword(
            @RequestBody @Validated(ResetPassword.class) UserDTO user) {
        return userService.resetPassword(user.id(), user.password(), user.currPassword())
                .map(RespWrapper::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping
    public Mono<RespWrapper<UserVO>> update(@RequestBody @Validated(Update.class) UserDTO user) {
        return userService.update(userConverter.toUpdateEntity(user)).map(RespWrapper::success);
    }
}
