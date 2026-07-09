package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.channel.SendReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final TemplateEngine templateEngine;
    private final ChannelManager channelManager;

    public <T extends SendReq> Mono<String> send(T req) {
        if (req == null || req.getType() == null) {
            return Mono.empty();
        }
        return templateEngine.fill(req)
                .flatMap(r -> channelManager.chose(r)
                        .flatMap(channel -> invokeChannelSend(channel, r)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Mono<String> invokeChannelSend(AbstractChannel<?> channel, SendReq req) {
        return ((AbstractChannel) channel).send(req);
    }
}
