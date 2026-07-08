package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class AbstractChannel<T extends SendReq> {

    private final String id;
    private final String code;
    private final ChannelType type;
    private final ChannelProvider provider;
    private final boolean enabled;

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected AbstractChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.type = entity.getType();
        this.provider = entity.getProvider();
        this.enabled = entity.isEnabled();

        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public final Mono<String> send(T req) {
        return doSend(req);
    }

    protected abstract Mono<String> doSend(T req);

    /**
     * @deprecated 随 {@link com.github.waitlight.asskicker.dto.UniTask} 废弃而废弃，改用 {@link #send(SendReq)}
     */
    @Deprecated
    public final Mono<String> send(UniTask task) {
        return Mono.empty();
    }

    public abstract void dispose();
}
