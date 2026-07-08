package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.RecordEntity;
import com.github.waitlight.asskicker.model.SendRecordStatus;
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

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;
    protected final RecordService recordService;

    protected AbstractChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper,
                              RecordService recordService) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.type = entity.getType();
        this.provider = entity.getProvider();

        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.recordService = recordService;
    }

    public final Mono<String> send(T req) {
        return doSend(req)
                .doOnSuccess(result -> writeRecord(req, SendRecordStatus.SUCCESS, null))
                .doOnError(error -> writeRecord(req, SendRecordStatus.FAILED, error.getMessage()));
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

    private void writeRecord(SendReq req, SendRecordStatus status, String errorMessage) {
        RecordEntity record = new RecordEntity();
        record.setTemplateCode(req.getTemplateCode());
        record.setLanguageCode(req.getLanguage() != null ? req.getLanguage().getCode() : null);
        if (req.getTemplateParams() != null) {
            record.setParams(new HashMap<>(req.getTemplateParams()));
        }
        record.setChannelId(id);
        record.setChannelType(type);
        record.setChannelName(code);
        record.setStatus(status);
        record.setErrorMessage(errorMessage);
        record.setSentAt(System.currentTimeMillis());
        recordService.create(record);
    }
}
