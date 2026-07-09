package com.github.waitlight.asskicker.controller;

import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.OpenApiConfig;
import com.github.waitlight.asskicker.dto.Resp;
import com.github.waitlight.asskicker.dto.send.SendVO;
import com.github.waitlight.asskicker.mq.SendReqProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Tag(name = "发送消息")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class SendController {

    private final SendReqProducer sendReqProducer;

    @Operation(summary = "发送消息", security = @SecurityRequirement(name = OpenApiConfig.BEARER_JWT))
    @PostMapping("/send")
    public Mono<Resp<SendVO>> sendDirect(@Valid @RequestBody SendReq req) {
        req.setRecordId(ObjectId.get().toString());
        return sendReqProducer.publish(req)
                .map(SendVO::new)
                .map(Resp::success)
                .onErrorResume(ResponseStatusException.class, ex -> Mono.just(
                        Resp.error(String.valueOf(ex.getStatusCode().value()),
                                ex.getReason() == null ? "发送失败" : ex.getReason())));
    }
}
