package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.channel.ChannelDebugResultVO;
import com.github.waitlight.asskicker.dto.channel.ChannelDebugSendDTO;
import com.github.waitlight.asskicker.service.ChannelDebugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "渠道调试")
@RestController
@RequestMapping("/v1/channels/debug")
@RequiredArgsConstructor
@Validated
public class ChannelDebugController {

    private final ChannelDebugService service;

    @Operation(summary = "测试渠道发送", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping
    public Mono<Resp<ChannelDebugResultVO>> send(@Valid @RequestBody ChannelDebugSendDTO dto) {
        return service.send(dto.getChannelId(), dto.getRequest())
                .map(Resp::success);
    }
}
