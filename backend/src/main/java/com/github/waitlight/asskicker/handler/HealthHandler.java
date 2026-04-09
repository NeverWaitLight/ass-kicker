package com.github.waitlight.asskicker.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class HealthHandler {

    public Mono<ServerResponse> health(ServerRequest request) {
        return ServerResponse.ok().bodyValue(Map.of("status", "UP"));
    }
}
