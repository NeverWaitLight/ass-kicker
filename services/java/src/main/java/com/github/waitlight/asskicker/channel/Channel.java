package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class Channel<T extends SendReq> {

    private final String id;
    private final String code;
    private final ChannelType channelType;
    private final boolean enabled;

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected Channel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.channelType = entity.getType();
        this.enabled = entity.isEnabled();

        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public abstract Mono<String> send(T req);

    public final Mono<String> send(UniTask task) {
        return Mono.empty();
    }

    public void dispose() {
        // no-op by default; subclasses holding long-lived resources should override
    }
}
