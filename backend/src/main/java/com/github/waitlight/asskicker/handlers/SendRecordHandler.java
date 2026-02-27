package com.github.waitlight.asskicker.handlers;

import com.github.waitlight.asskicker.service.SendRecordService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class SendRecordHandler {

    private final SendRecordService sendRecordService;

    public SendRecordHandler(SendRecordService sendRecordService) {
        this.sendRecordService = sendRecordService;
    }

    public Mono<ServerResponse> listRecords(ServerRequest request) {
        int page = parseInt(request.queryParam("page").orElse("1"), 1);
        int size = parseInt(request.queryParam("size").orElse("10"), 10);
        String recipient = request.queryParam("recipient").filter(s -> !s.isBlank()).orElse(null);
        String channelType = request.queryParam("channelType").filter(s -> !s.isBlank()).orElse(null);
        return sendRecordService.listRecords(page, size, recipient, channelType)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取发送记录列表失败" : ex.getReason()));
    }

    public Mono<ServerResponse> getRecordById(ServerRequest request) {
        String id = request.pathVariable("id");
        return sendRecordService.getById(id)
                .flatMap(record -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(record))
                .onErrorResume(ResponseStatusException.class, ex ->
                        ServerResponse.status(ex.getStatusCode())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(ex.getReason() == null ? "获取发送记录失败" : ex.getReason()));
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
