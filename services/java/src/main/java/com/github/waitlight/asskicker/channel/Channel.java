package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class Channel {

    private final String id;
    private final String code;
    private final ChannelType channelType;
    private final boolean enabled;

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected Channel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.channelType = entity.getChannelType();
        this.enabled = entity.isEnabled();

        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public final Mono<String> send(UniTask task) {
        if (task == null) {
            return Mono.error(new IllegalArgumentException("task required"));
        }
        return doSend(task.getMessage(), task.getAddress());
    }

    protected abstract Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress);
}
