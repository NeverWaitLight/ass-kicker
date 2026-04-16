package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.converter.UserConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.user.CreateUserDTO;
import com.github.waitlight.asskicker.dto.user.ResetPasswordDTO;
import com.github.waitlight.asskicker.dto.user.UpdateUserDTO;
import com.github.waitlight.asskicker.dto.user.UserVO;
import com.github.waitlight.asskicker.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

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
    public Mono<Resp<UserVO>> create(@RequestBody @Validated CreateUserDTO user) {
        user = new CreateUserDTO(user.username().trim(), user.password(), user.role());
        return userService.create(userConverter.toEntity(user))
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PatchMapping
    public Mono<Resp<UserVO>> update(@RequestBody @Validated UpdateUserDTO user) {
        return userService.update(userConverter.toEntity(user))
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "resetPassword", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/password")
    public Mono<Resp<UserVO>> resetPassword(
            @RequestBody @Validated ResetPasswordDTO user) {
        return userService.resetPassword(user.id(), user.password(), user.currPassword())
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @NotBlank String id) {
        return userService.delete(id);
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<UserVO>> getById(@PathVariable String id) {
        return userService.getById(id)
                .map(userConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<UserVO>> page(@Validated PageReq pageReq) {
        int page = pageReq.getPage();
        int size = pageReq.getSize();
        String keyword = pageReq.getKeyword();
        int offset = (page - 1) * size;

        return userService.count(keyword)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageResp.success(page, size, total, List.of()));
                    }
                    return userService.list(keyword, size, offset)
                            .map(userConverter::toVO)
                            .collectList()
                            .map(users -> PageResp.success(page, size, total, users));
                });
    }
}
