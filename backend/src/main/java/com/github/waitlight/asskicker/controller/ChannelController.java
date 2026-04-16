package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.converter.ChannelConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.channel.CreateChannelDTO;
import com.github.waitlight.asskicker.dto.channel.ChannelPropertiesSchemaVO;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.dto.channel.ChannelVO;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.dto.channel.UpdateChannelDTO;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.service.ChannelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.concurrent.TimeUnit;

@Tag(name = "ChannelController")
@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
@Validated
public class ChannelController {

        private final ChannelService channelService;
        private final ChannelConverter channelConverter;
        private final ChannelManager channelManager;

        @Operation(summary = "create", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PostMapping
        public Mono<Resp<ChannelVO>> create(@Valid @RequestBody CreateChannelDTO request) {
                channelManager.validateProperties(request.getProvider(), request.getProperties());

                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(channelService::create)
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping
        public Mono<PageResp<ChannelVO>> page(@Validated PageReq pageReq) {
                int page = pageReq.getPage();
                int size = pageReq.getSize();
                String keyword = pageReq.getKeyword();
                int offset = (page - 1) * size;

                return channelService.count(keyword)
                                .flatMap(total -> {
                                        if (total == 0) {
                                                return Mono.just(PageResp.success(page, size, total, List.of()));
                                        }
                                        return channelService.list(keyword, size, offset)
                                                        .map(channelConverter::toVO)
                                                        .collectList()
                                                        .map(channels -> PageResp.success(page, size, total, channels));
                                });
        }

        @Operation(summary = "channelTypes", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/types")
        public Mono<ResponseEntity<Resp<List<ChannelType>>>> channelTypes() {
                return Mono.fromSupplier(() -> List.of(ChannelType.values()))
                                .map(Resp::success)
                                .map(body -> ResponseEntity.ok()
                                                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                                                .body(body));
        }

        @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{id}")
        public Mono<Resp<ChannelVO>> getById(@PathVariable String id) {
                return channelService.getById(id)
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "providerProperties", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/providers/{providerType}/properties")
        public Mono<Resp<ChannelPropertiesSchemaVO>> providerProperties(@PathVariable ProviderType providerType) {
                return Mono.fromSupplier(() -> ChannelPropertiesSchemaVO.builder()
                                .properties(channelManager.getPropertiesSchema(providerType))
                                .build())
                                .map(Resp::success);
        }

        @Operation(summary = "providersByChannelType", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/types/{channelType}/providers")
        public Mono<ResponseEntity<Resp<List<ChannelProviderOptionVO>>>> providersByChannelType(
                        @PathVariable ChannelType channelType) {
                return Mono.fromSupplier(() -> channelManager.getProvidersByChannelType(channelType))
                                .map(Resp::success)
                                .map(body -> ResponseEntity.ok()
                                                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                                                .body(body));
        }

        @Operation(summary = "update", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PutMapping
        public Mono<Resp<ChannelVO>> update(@Valid @RequestBody UpdateChannelDTO request) {
                channelManager.validateProperties(request.getProvider(), request.getProperties());

                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(patch -> channelService.update(request.getId(), patch))
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "delete", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @DeleteMapping("/{id}")
        public Mono<Void> delete(@PathVariable String id) {
                return channelService.delete(id);
        }
}