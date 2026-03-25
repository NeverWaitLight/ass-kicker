package com.github.waitlight.asskicker;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniSendMessageReq;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@AllArgsConstructor
public class Sender {

    private final MessageTemplateEngine messageTemplateHandler;

    public Mono<List<String>> send(UniSendMessageReq req, UniAddress... uniAddresses) {
        return messageTemplateHandler.fill(req).thenReturn(List.of("success"));
    }

}
