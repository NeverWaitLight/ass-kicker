package com.github.waitlight.asskicker.handler;

import com.github.waitlight.asskicker.dto.send.SendRequest;
import com.github.waitlight.asskicker.dto.send.SendResponse;
import com.github.waitlight.asskicker.manager.SendTaskExecutor;
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
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SendHandler {

    private final SendTaskExecutor sendTaskExecutor;
    private final SendTaskProducer sendTaskProducer;

    public Mono<ServerResponse> send(ServerRequest request) {
        return bodyToSubmitRequest(request)
                .flatMap(this::buildAndSend)
                .flatMap(taskId -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new SendResponse(taskId)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse.status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "发送失败" : ex.getReason()));
    }

    public Mono<ServerResponse> submit(ServerRequest request) {
        return bodyToSubmitRequest(request)
                .flatMap(this::validateAndPublish)
                .flatMap(taskId -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new SendResponse(taskId)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse.status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "提交失败" : ex.getReason()));
    }

    private Mono<SendRequest> bodyToSubmitRequest(ServerRequest request) {
            return request.bodyToMono(SendRequest.class)
                .onErrorMap(ServerWebInputException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .onErrorMap(DecodingException.class,
                        ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"));
    }

    private Mono<String> buildAndSend(SendRequest body) {
        if (body == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空"));
        }
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> params = body.params() != null ? body.params() : Map.of();
        long submittedAt = Instant.now().toEpochMilli();
        SendRequest task = new SendRequest(
                body.templateCode(),
                body.language(),
                params,
                body.recipients(),
                taskId,
                submittedAt);
        sendTaskExecutor.submit(task);
        return Mono.just(taskId);
    }

    private Mono<String> validateAndPublish(SendRequest body) {
        if (body == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空"));
        }
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> params = body.params() != null ? body.params() : Map.of();
        long submittedAt = Instant.now().toEpochMilli();
        SendRequest task = new SendRequest(
                body.templateCode(),
                body.language(),
                params,
                body.recipients(),
                taskId,
                submittedAt);
        return sendTaskProducer.publish(task).thenReturn(taskId);
    }
}
