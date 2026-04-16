package com.github.waitlight.asskicker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.waitlight.asskicker.config.openapi.OpenApiConfig;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.record.RecordVO;
import com.github.waitlight.asskicker.service.RecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Tag(name = "RecordController")
@RestController
@RequestMapping("/v1/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "page", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageResp<RecordVO>> page(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码，从1开始", example = "1") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页大小", example = "10") int size,
            @RequestParam(required = false) @Parameter(description = "收件人") String recipient,
            @RequestParam(required = false) @Parameter(description = "渠道类型") String channelType) {

        return recordService.page(page, size, recipient, channelType)
                .map(pr -> PageResp.success(pr.page(), pr.size(), pr.total(), pr.data()));
    }

    @Operation(summary = "getById", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<Resp<RecordVO>> getById(
            @PathVariable @Parameter(description = "发送记录ID") String id) {

        return recordService.getById(id)
                .map(Resp::success);
    }
}
