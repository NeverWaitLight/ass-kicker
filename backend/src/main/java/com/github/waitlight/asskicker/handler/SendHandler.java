package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.Sender;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.dto.send.SendResponse;
import com.github.waitlight.asskicker.mq.SendTaskProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SendHandler {

    private final SendTaskProducer sendTaskProducer;
    private final Sender sender;

    public Mono<ServerResponse> send(ServerRequest request) {
        return bodyToSendRequest(request)
                .flatMap(this::validateAndExecute)
                .flatMap(taskId -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new SendResponse(taskId)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "发送失败" : ex.getReason()));
    }

    public Mono<ServerResponse> submit(ServerRequest request) {
        return bodyToSendRequest(request)
                .flatMap(this::validateAndPublish)
                .flatMap(taskId -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new SendResponse(taskId)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse
                        .status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "提交失败" : ex.getReason()));
    }

    private Mono<UniTask> bodyToSendRequest(ServerRequest request) {
        return request.bodyToMono(UniTask.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"));
    }

    private Mono<String> validateAndExecute(UniTask rawTask) {
        UniTask task = validateAndEnrich(rawTask);
        return sender.send(task);
    }

    private Mono<String> validateAndPublish(UniTask rawTask) {
        UniTask task = validateAndEnrich(rawTask);
        return sendTaskProducer.publish(task).thenReturn(task.getTaskId());
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
