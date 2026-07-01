package com.github.waitlight.asskicker.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.converter.ChannelConverter;
import com.github.waitlight.asskicker.dto.PageReq;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.channel.CreateChannelDTO;
import com.github.waitlight.asskicker.dto.channel.ChannelProviderOptionVO;
import com.github.waitlight.asskicker.dto.channel.ChannelVO;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.dto.channel.UpdateChannelDTO;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "渠道管理")
@RestController
@RequestMapping("/v1/channels")
@RequiredArgsConstructor
@Validated
public class ChannelController {

        private final ChannelService channelService;
        private final ChannelConverter channelConverter;
        private final ChannelManager channelManager;

        @Operation(summary = "创建渠道", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PostMapping
        public Mono<Resp<ChannelVO>> create(@Valid @RequestBody CreateChannelDTO request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(entity -> channelService.create(entity, principal.userId()))
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "分页查询", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping
        public Mono<PageResp<ChannelVO>> page(@Validated PageReq pageReq,
                        @RequestParam(required = false) ChannelType channelType,
                        @RequestParam(required = false) ProviderType providerType) {
                int page = pageReq.getPage();
                int size = pageReq.getSize();
                String keyword = pageReq.getKeyword();
                int offset = (page - 1) * size;

                return channelService.count(keyword, channelType, providerType)
                                .flatMap(total -> {
                                        if (total == 0) {
                                                return Mono.just(PageResp.success(page, size, total, List.of()));
                                        }
                                        return channelService.list(keyword, channelType, providerType, size, offset)
                                                        .map(channelConverter::toVO)
                                                        .collectList()
                                                        .map(channels -> PageResp.success(page, size, total, channels));
                                });
        }

        @Operation(summary = "渠道类型列表", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/types")
        public Mono<Resp<List<ChannelType>>> channelTypes() {
                return Mono.fromSupplier(() -> List.of(ChannelType.values()))
                                .map(Resp::success);
        }

        @Operation(summary = "查询渠道", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{id}")
        public Mono<Resp<ChannelVO>> getById(@PathVariable String id) {
                return channelService.getById(id)
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "按渠道类型查询服务商", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @GetMapping("/{type}/providers")
        public Mono<Resp<List<ChannelProviderOptionVO>>> providersByChannelType(
                        @PathVariable ChannelType type) {
                return Mono.fromSupplier(() -> channelManager.getProvidersByChannelType(type))
                                .map(Resp::success);
        }

        @Operation(summary = "更新渠道", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @PutMapping
        public Mono<Resp<ChannelVO>> update(@Valid @RequestBody UpdateChannelDTO request,
                        @AuthenticationPrincipal UserPrincipal principal) {
                return Mono.just(request)
                                .map(channelConverter::toEntity)
                                .flatMap(patch -> channelService.update(request.getId(), patch, principal.userId()))
                                .map(channelConverter::toVO)
                                .map(Resp::success);
        }

        @Operation(summary = "删除渠道", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
        @DeleteMapping("/{id}")
        public Mono<Void> delete(@PathVariable String id) {
                return channelService.delete(id);
        }

}