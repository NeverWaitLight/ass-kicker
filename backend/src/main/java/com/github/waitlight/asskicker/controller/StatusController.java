package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.dto.RespWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "StatusController")
@RestController
@RequestMapping("/status")
public class StatusController {

    @Operation(summary = "get")
    @GetMapping
    public Mono<RespWrapper<Object>> get() {
        return Mono.just(RespWrapper.success(Map.of("service", "ass-kicker", "status", "OK")));
    }
}