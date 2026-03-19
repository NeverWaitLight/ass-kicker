package com.github.waitlight.asskicker.service.impl;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordPageResponse;
import com.github.waitlight.asskicker.dto.sendrecord.SendRecordView;
import com.github.waitlight.asskicker.model.SendRecordEntity;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import com.github.waitlight.asskicker.service.SendRecordService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SendRecordServiceImpl implements SendRecordService, DisposableBean {

    private final SendRecordRepository sendRecordRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;

    @Value("${send-record.buffer-size:100}")
    private int bufferSize;

    @Value("${send-record.flush-interval-ms:5000}")
    private long flushIntervalMs;

    private final List<SendRecordEntity> buffer = Collections.synchronizedList(new ArrayList<>());

    private AsyncLoadingCache<String, Optional<SendRecordView>> recordByIdCache;

    public SendRecordServiceImpl(SendRecordRepository sendRecordRepository,
                                 CaffeineCacheConfig caffeineCacheConfig) {
        this.sendRecordRepository = sendRecordRepository;
        this.caffeineCacheConfig = caffeineCacheConfig;
    }

    @PostConstruct
    void initCaches() {
        recordByIdCache = caffeineCacheConfig.buildCache((id, executor) ->
                sendRecordRepository.findById(id)
                        .map(r -> Optional.of(toView(r)))
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    @Override
    public Mono<SendRecordPageResponse> listRecords(int page, int size, String recipient, String channelType) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int offset = (normalizedPage - 1) * normalizedSize;
        String recipientFilter = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        String channelTypeFilter = (channelType != null && !channelType.isBlank()) ? channelType.trim() : null;

        Mono<Long> totalMono = sendRecordRepository.countAll(recipientFilter, channelTypeFilter);
        Mono<List<SendRecordView>> itemsMono = sendRecordRepository.findPage(normalizedSize, offset, recipientFilter, channelTypeFilter)
                .map(this::toView)
                .collectList();

        return Mono.zip(itemsMono, totalMono)
                .map(tuple -> new SendRecordPageResponse(tuple.getT1(), normalizedPage, normalizedSize, tuple.getT2()));
    }

    @Override
    public Mono<SendRecordView> getById(String id) {
        return Mono.fromFuture(recordByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "发送记录不存在"))));
    }

    @Override
    public void writeRecord(SendRecordEntity record) {
        List<SendRecordEntity> toFlush = null;
        synchronized (buffer) {
            buffer.add(record);
            if (buffer.size() >= bufferSize) {
                toFlush = new ArrayList<>(buffer);
                buffer.clear();
            }
        }
        if (toFlush != null && !toFlush.isEmpty()) {
            flushAsync(toFlush);
        }
    }

    @Scheduled(fixedDelayString = "${send-record.flush-interval-ms:5000}")
    public void flushScheduled() {
        List<SendRecordEntity> toFlush = null;
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        if (toFlush != null && !toFlush.isEmpty()) {
            flushAsync(toFlush);
        }
    }

    @Override
    public void destroy() {
        List<SendRecordEntity> toFlush;
        synchronized (buffer) {
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        if (toFlush != null && !toFlush.isEmpty()) {
            sendRecordRepository.saveAll(toFlush)
                    .doOnError(e -> log.error("SEND_RECORD_BATCH_SAVE_FAILED on shutdown size={} error={}", toFlush.size(), e.getMessage()))
                    .blockLast();
        }
    }

    private void flushAsync(List<SendRecordEntity> batch) {
        sendRecordRepository.saveAll(batch)
                .doOnError(e -> log.error("SEND_RECORD_BATCH_SAVE_FAILED size={} error={}", batch.size(), e.getMessage()))
                .subscribe();
    }

    private SendRecordView toView(SendRecordEntity r) {
        return new SendRecordView(
                r.getId(),
                r.getTaskId(),
                r.getTemplateCode(),
                r.getLanguageCode(),
                r.getParams(),
                r.getChannelId(),
                r.getRecipients(),
                r.getRecipient(),
                r.getSubmittedAt(),
                r.getRenderedContent(),
                r.getChannelType(),
                r.getChannelName(),
                r.getStatus(),
                r.getErrorCode(),
                r.getErrorMessage(),
                r.getSentAt()
        );
    }
}
