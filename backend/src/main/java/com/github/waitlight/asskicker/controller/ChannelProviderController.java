package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderDTO;
import com.github.waitlight.asskicker.service.ChannelProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "ChannelProviderController")
@RestController
@RequestMapping("/v1/channel-providers")
@RequiredArgsConstructor
public class ChannelProviderController {

    private final ChannelProviderService channelProviderService;
    private final ChannelProviderConverter channelProviderConverter;
    private final Validator validator;

    @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Resp<ChannelProviderDTO>> create(@RequestBody ChannelProviderDTO request) {
        return validateDto(request)
                .map(channelProviderConverter::toEntity)
                .flatMap(channelProviderService::create)
                .map(channelProviderConverter::toDto)
                .map(Resp::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        Resp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "创建通道商失败" : ex.getReason())));
    }

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<ChannelProviderDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return channelProviderService.page(page, size)
                .map(pr -> PageResp.success(pr.page(), pr.size(), pr.total(), pr.data()))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        PageResp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "获取通道商列表失败" : ex.getReason())));
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<ChannelProviderDTO>> getById(@PathVariable String id) {
        return channelProviderService.findById(id)
                .map(channelProviderConverter::toDto)
                .map(Resp::success)
                .switchIfEmpty(Mono.defer(() -> Mono.just(Resp.error(String.valueOf(HttpStatus.NOT_FOUND.value()), "未找到通道商"))))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        Resp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "获取通道商失败" : ex.getReason())));
    }

    @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}")
    public Mono<Resp<ChannelProviderDTO>> update(
            @PathVariable String id,
            @RequestBody ChannelProviderDTO request) {
        return validateDto(request)
                .map(channelProviderConverter::toEntity)
                .flatMap(patch -> channelProviderService.update(id, patch))
                .map(channelProviderConverter::toDto)
                .map(Resp::success)
                .switchIfEmpty(Mono.defer(() -> Mono.just(Resp.error(String.valueOf(HttpStatus.NOT_FOUND.value()), "未找到要更新的通道商"))))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        Resp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "更新通道商失败" : ex.getReason())));
    }

    @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return channelProviderService.delete(id)
                .onErrorResume(ResponseStatusException.class, ex -> {
                    // 如果删除失败，抛出异常
                    return Mono.error(new ResponseStatusException(
                            ex.getStatusCode(),
                            ex.getReason() == null ? "删除通道商失败" : ex.getReason()));
                });
    }

    private Mono<ChannelProviderDTO> validateDto(ChannelProviderDTO dto) {
        Set<ConstraintViolation<ChannelProviderDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, message));
        }
        return Mono.just(dto);
    }
}