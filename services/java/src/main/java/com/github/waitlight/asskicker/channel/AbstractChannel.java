package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.*;
import com.github.waitlight.asskicker.service.RecordService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Getter
@Slf4j
public abstract class AbstractChannel<T extends SendReq> {

    private final String id;
    private final String code;
    private final ChannelType type;
    private final ChannelProvider provider;
    private final ChannelEntity channel;

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;
    protected final RecordService recordService;

    protected AbstractChannel(ChannelEntity channel, WebClient webClient, ObjectMapper objectMapper,
                              RecordService recordService) {
        this.id = channel.getId();
        this.code = channel.getCode();
        this.type = channel.getType();
        this.provider = channel.getProvider();
        this.channel = channel;

        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.recordService = recordService;
    }

    public final Mono<String> send(T req) {
        return doSend(req)
                .map(result -> recording(req, SendRecordStatus.SUCCESS, null))
                .doOnError(error -> recording(req, SendRecordStatus.FAILED, error.getMessage()));
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

    private String recording(SendReq req, SendRecordStatus status, String errorMessage) {
        RecordEntity r = new RecordEntity();
        r.setTemplateCode(req.getTemplateCode());
        r.setLanguageCode(req.getLanguage() != null ? req.getLanguage().getCode() : null);
        if (req.getTemplateParams() != null) {
            r.setParams(new HashMap<>(req.getTemplateParams()));
        }
        r.setChannelId(id);
        r.setChannelType(type);
        r.setChannelName(channel.getName());
        r.setRecipient(req.recipient());
        r.setRenderedContent(req.renderedContent());
        r.setDirectSend(req.isDirectSend());
        r.setStatus(status);
        r.setErrorMessage(errorMessage);
        r.setSentAt(System.currentTimeMillis());
        return recordService.create(r);
    }
}
