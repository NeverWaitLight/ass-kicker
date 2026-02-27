package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.dto.submit.SubmitRequest;
import com.github.waitlight.asskicker.dto.submit.SubmitResponse;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.mq.SendTaskProducer;

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
public class SubmitHandler {

    private final SendTaskProducer sendTaskProducer;

    public SubmitHandler(SendTaskProducer sendTaskProducer) {
        this.sendTaskProducer = sendTaskProducer;
    }

    public Mono<ServerResponse> submit(ServerRequest request) {
        return request.bodyToMono(SubmitRequest.class)
                .onErrorMap(ServerWebInputException.class, ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .onErrorMap(DecodingException.class, ex -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据格式错误"))
                .flatMap(this::validateAndPublish)
                .flatMap(taskId -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new SubmitResponse(taskId)))
                .onErrorResume(ResponseStatusException.class, ex -> ServerResponse.status(ex.getStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ex.getReason() == null ? "提交失败" : ex.getReason()));
    }

    private Mono<String> validateAndPublish(SubmitRequest body) {
        if (body == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空"));
        }
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> params = body.params() != null ? body.params() : Map.of();
        SendTask task = SendTask.builder()
                .taskId(taskId)
                .templateCode(body.templateCode())
                .languageCode(body.language().getCode())
                .params(params)
                .channelId(body.channelId())
                .recipients(body.recipients())
                .submittedAt(Instant.now().toEpochMilli())
                .build();
        return sendTaskProducer.publish(task).thenReturn(taskId);
    }
}
