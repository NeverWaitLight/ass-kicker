package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.PageRespWrapper;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordView;
import com.github.waitlight.asskicker.service.SendRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "SendRecordController")
@RestController
@RequestMapping("/v1/send-records")
@RequiredArgsConstructor
public class SendRecordController {

    private final SendRecordService sendRecordService;

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping
    public Mono<PageRespWrapper<SendRecordView>> listRecords(
            @RequestParam(defaultValue = "1")
            @Parameter(description = "页码，从1开始", example = "1")
            int page,
            @RequestParam(defaultValue = "10")
            @Parameter(description = "每页大小", example = "10")
            int size,
            @RequestParam(required = false)
            @Parameter(description = "收件人")
            String recipient,
            @RequestParam(required = false)
            @Parameter(description = "渠道类型")
            String channelType) {

        return sendRecordService.listRecords(page, size, recipient, channelType)
                .map(pr -> PageRespWrapper.success(pr.page(), pr.size(), pr.total(), pr.data()));
    }

    @Operation(security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @GetMapping("/{id}")
    public Mono<RespWrapper<SendRecordView>> getRecordById(
            @PathVariable
            @Parameter(description = "发送记录ID")
            String id) {

        return sendRecordService.getById(id)
                .map(RespWrapper::success);
    }
}