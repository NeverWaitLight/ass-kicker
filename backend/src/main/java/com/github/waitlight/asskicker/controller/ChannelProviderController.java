package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.ChannelProviderConverter;
import com.github.waitlight.asskicker.dto.PageRespWrapper;
import com.github.waitlight.asskicker.dto.RespWrapper;
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

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RespWrapper<ChannelProviderDTO>> createChannelProvider(@RequestBody ChannelProviderDTO request) {
        return validateDto(request)
                .map(channelProviderConverter::toEntity)
                .flatMap(channelProviderService::create)
                .map(channelProviderConverter::toDto)
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        RespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "创建通道商失败" : ex.getReason())));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageRespWrapper<ChannelProviderDTO>> listChannelProviders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return channelProviderService.listPage(page, size)
                .map(pr -> PageRespWrapper.success(pr.page(), pr.size(), pr.total(), pr.data()))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        PageRespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "获取通道商列表失败" : ex.getReason())));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<RespWrapper<ChannelProviderDTO>> getChannelProviderById(@PathVariable String id) {
        return channelProviderService.findById(id)
                .map(channelProviderConverter::toDto)
                .map(RespWrapper::success)
                .switchIfEmpty(Mono.defer(() -> Mono.just(RespWrapper.error(String.valueOf(HttpStatus.NOT_FOUND.value()), "未找到通道商"))))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        RespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "获取通道商失败" : ex.getReason())));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PutMapping("/{id}")
    public Mono<RespWrapper<ChannelProviderDTO>> updateChannelProvider(
            @PathVariable String id,
            @RequestBody ChannelProviderDTO request) {
        return validateDto(request)
                .map(channelProviderConverter::toEntity)
                .flatMap(patch -> channelProviderService.update(id, patch))
                .map(channelProviderConverter::toDto)
                .map(RespWrapper::success)
                .switchIfEmpty(Mono.defer(() -> Mono.just(RespWrapper.error(String.valueOf(HttpStatus.NOT_FOUND.value()), "未找到要更新的通道商"))))
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        RespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "更新通道商失败" : ex.getReason())));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteChannelProvider(@PathVariable String id) {
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