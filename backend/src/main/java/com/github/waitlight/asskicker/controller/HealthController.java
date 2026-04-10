package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.dto.Resp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "HealthController")
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    @Operation(summary = "health")
    @GetMapping
    public Mono<Resp<Object>> health() {
        return Mono.just(Resp.success(Map.of("status", "UP")));
    }
}