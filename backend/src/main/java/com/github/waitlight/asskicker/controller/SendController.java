package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.Sender;
import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.RespWrapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.dto.send.SendResponse;
import com.github.waitlight.asskicker.mq.SendTaskProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "SendController")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SendController {

    private final SendTaskProducer sendTaskProducer;
    private final Sender sender;

    @Operation(
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UniTask.class)
            )
        ),
        responses = @ApiResponse(
            responseCode = "200",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RespWrapper.class)
            )
        )
    )
    @PostMapping("/send")
    public Mono<RespWrapper<SendResponse>> send(@Valid @RequestBody UniTask rawTask) {
        return Mono.just(rawTask)
                .map(this::validateAndEnrich)
                .flatMap(sender::send)
                .map(SendResponse::new)
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                    RespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                        ex.getReason() == null ? "发送失败" : ex.getReason())));
    }

    @Operation(
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UniTask.class)
            )
        ),
        responses = @ApiResponse(
            responseCode = "200",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RespWrapper.class)
            )
        )
    )
    @PostMapping("/submit")
    public Mono<RespWrapper<SendResponse>> submit(@Valid @RequestBody UniTask rawTask) {
        return Mono.just(rawTask)
                .map(this::validateAndEnrich)
                .flatMap(task -> sendTaskProducer.publish(task).thenReturn(task.getTaskId()))
                .map(SendResponse::new)
                .map(RespWrapper::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                    RespWrapper.error(String.valueOf(ex.getStatusCode().value()),
                        ex.getReason() == null ? "提交失败" : ex.getReason())));
    }

    private UniTask validateAndEnrich(UniTask task) {
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }

        UniMessage message = task.getMessage();
        UniAddress address = task.getAddress();
        if (message == null || address == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message 和 address 不能为空");
        }
        if (message.getTemplateCode() == null || message.getTemplateCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateCode 不能为空");
        }
        if (message.getLanguage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "language 不能为空");
        }
        if (address.getChannelType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "channelType 不能为空");
        }
        if (address.getRecipients() == null || address.getRecipients().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipients 不能为空");
        }

        return UniTask.builder()
                .message(message)
                .address(address)
                .taskId(task.getTaskId() == null || task.getTaskId().isBlank()
                        ? UUID.randomUUID().toString()
                        : task.getTaskId())
                .submittedAt(task.getSubmittedAt() == null
                        ? Instant.now().toEpochMilli()
                        : task.getSubmittedAt())
                .build();
    }
}