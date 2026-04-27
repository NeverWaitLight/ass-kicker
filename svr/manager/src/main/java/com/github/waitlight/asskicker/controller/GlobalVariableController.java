package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.converter.GlobalVariableConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.globalvariable.CreateGlobalVariableDTO;
import com.github.waitlight.asskicker.dto.globalvariable.GlobalVariableVO;
import com.github.waitlight.asskicker.dto.globalvariable.UpdateGlobalVariableDTO;
import com.github.waitlight.asskicker.service.GlobalVariableService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "GlobalVariableController")
@RestController
@RequestMapping("/v1/global-variables")
@RequiredArgsConstructor
@Validated
public class GlobalVariableController {

    private final GlobalVariableService globalVariableService;
    private final GlobalVariableConverter globalVariableConverter;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<GlobalVariableVO>> create(@Valid @RequestBody CreateGlobalVariableDTO request) {
        return Mono.just(request)
                .map(globalVariableConverter::toEntity)
                .flatMap(globalVariableService::create)
                .map(globalVariableConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping
    public Mono<Resp<GlobalVariableVO>> update(@Valid @RequestBody UpdateGlobalVariableDTO request) {
        return Mono.just(request)
                .map(globalVariableConverter::toEntity)
                .flatMap(patch -> globalVariableService.update(request.getId(), patch))
                .map(globalVariableConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable @NotBlank String id) {
        return globalVariableService.delete(id);
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<GlobalVariableVO>> getById(@PathVariable String id) {
        return globalVariableService.findById(id)
                .map(globalVariableConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "getByKey", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/key/{key}")
    public Mono<Resp<GlobalVariableVO>> getByKey(@PathVariable String key) {
        return globalVariableService.findByKey(key)
                .map(globalVariableConverter::toVO)
                .map(Resp::success);
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<GlobalVariableVO>> page(@Validated PageReq pageReq) {
        int page = pageReq.getPage();
        int size = pageReq.getSize();
        String keyword = pageReq.getKeyword();
        int offset = (page - 1) * size;

        return globalVariableService.count(keyword)
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(PageResp.success(page, size, total, List.of()));
                    }
                    return globalVariableService.list(keyword, size, offset)
                            .map(globalVariableConverter::toVO)
                            .collectList()
                            .map(variables -> PageResp.success(page, size, total, variables));
                });
    }
}
