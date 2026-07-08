package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.channel.ChannelReq;
import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.dto.send.SendVO;
import com.github.waitlight.asskicker.exception.BusinessException;
import com.github.waitlight.asskicker.service.Sender;
import com.github.waitlight.asskicker.service.TemplateEngine;
import com.github.waitlight.asskicker.mq.SendTaskProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

import org.bson.types.ObjectId;

@Tag(name = "发送消息")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SendController {

    private final SendTaskProducer sendTaskProducer;
    private final TemplateEngine templateEngine;
    private final Sender sender;

    @Operation(summary = "发送消息", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping("/send")
    public Mono<Resp<SendVO>> send(@Valid @RequestBody UniTask rawTask) {
        return Mono.just(rawTask)
                .map(this::validateAndEnrich)
                .flatMap(task -> validateTemplateVariables(task)
                        .thenReturn(task))
                .flatMap(task -> sendTaskProducer.publish(task).thenReturn(task.getTaskId()))
                .map(SendVO::new)
                .map(Resp::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                    Resp.error(String.valueOf(ex.getStatusCode().value()),
                        ex.getReason() == null ? "发送失败" : ex.getReason())));
    }

    @Operation(summary = "直接发送(不经模板/MQ)", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping("/send/direct")
    public Mono<Resp<SendVO>> sendDirect(@Valid @RequestBody ChannelReq req) {
        if (req == null) {
            return Mono.just(Resp.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), "请求体不能为空"));
        }
        if (req.getType() == null) {
            return Mono.just(Resp.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), "channelType 不能为空"));
        }
        return sender.send(req)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "无可用渠道: channelType=" + req.getType()
                                + (req.getProvider() == null ? "" : ", provider=" + req.getProvider()))))
                .map(SendVO::new)
                .map(Resp::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        Resp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "发送失败" : ex.getReason())));
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
                        ? ObjectId.get().toString()
                        : task.getTaskId())
                .submittedAt(task.getSubmittedAt() == null
                        ? Instant.now().toEpochMilli()
                        : task.getSubmittedAt())
                .build();
    }

    private Mono<Void> validateTemplateVariables(UniTask task) {
        return templateEngine.findMissingVariables(task.getMessage())
                .onErrorMap(BusinessException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板不存在或语言内容不存在", ex))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板不存在或语言内容不存在")))
                .flatMap(missingVariables -> {
                    if (missingVariables.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.error(missingVariablesError(missingVariables));
                });
    }

    private ResponseStatusException missingVariablesError(Set<String> missingVariables) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "模板变量未填充: " + String.join(", ", missingVariables));
    }
}
