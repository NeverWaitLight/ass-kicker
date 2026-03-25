package com.github.waitlight.asskicker.manager;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.dto.send.SendRequest;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.MessageTemplateEntity;
import com.github.waitlight.asskicker.service.MessageTemplateService;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class MessageTemplateFactory {

    private final MessageTemplateService msgTmpService;

    public Mono<String> fill(SendRequest req) {
        return fill(req.templateCode(), req.language(), req.params());
    }

    public Mono<String> fill(String code, Language language, Map<String, Object> params) {
        Mono<MessageTemplateEntity> template = msgTmpService.findByCode(code);
        return Mono.justOrEmpty(null);
    }

}
