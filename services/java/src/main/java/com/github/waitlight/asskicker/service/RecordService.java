package com.github.waitlight.asskicker.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.waitlight.asskicker.config.CaffeineCacheConfig;
import com.github.waitlight.asskicker.converter.RecordConverter;
import com.github.waitlight.asskicker.dto.PageResp;
import com.github.waitlight.asskicker.dto.record.RecordVO;
import com.github.waitlight.asskicker.model.RecordEntity;
import com.github.waitlight.asskicker.repository.RecordRepository;
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
public class RecordService implements DisposableBean {

    private final RecordRepository recordRepository;
    private final CaffeineCacheConfig caffeineCacheConfig;
    private final RecordConverter recordConverter;
    private final List<RecordEntity> buffer = Collections.synchronizedList(new ArrayList<>());
    @Value("${send-record.buffer-size:100}")
    private int bufferSize;
    @Value("${send-record.flush-interval-ms:5000}")
    private long flushIntervalMs;
    private AsyncLoadingCache<String, Optional<RecordVO>> recordByIdCache;

    public RecordService(RecordRepository recordRepository,
                         CaffeineCacheConfig caffeineCacheConfig,
                         RecordConverter recordConverter) {
        this.recordRepository = recordRepository;
        this.caffeineCacheConfig = caffeineCacheConfig;
        this.recordConverter = recordConverter;
    }

    @PostConstruct
    void initCaches() {
        recordByIdCache = caffeineCacheConfig.buildCache((id, executor) ->
                recordRepository.findById(id)
                        .map(r -> Optional.of(recordConverter.toVO(r)))
                        .defaultIfEmpty(Optional.empty())
                        .toFuture());
    }

    public Mono<PageResp<RecordVO>> page(int page, int size, String recipient, String channelType) {
        int normalizedPage = page <= 0 ? 1 : page;
        int normalizedSize = size <= 0 ? 10 : size;
        int offset = (normalizedPage - 1) * normalizedSize;
        String recipientFilter = (recipient != null && !recipient.isBlank()) ? recipient.trim() : null;
        String channelTypeFilter = (channelType != null && !channelType.isBlank()) ? channelType.trim() : null;

        Mono<Long> totalMono = recordRepository.countAll(recipientFilter, channelTypeFilter);
        Mono<List<RecordVO>> itemsMono = recordRepository.findPage(normalizedSize, offset, recipientFilter, channelTypeFilter)
                .map(recordConverter::toVO)
                .collectList();

        return Mono.zip(itemsMono, totalMono)
                .map(tuple -> PageResp.success(normalizedPage, normalizedSize, tuple.getT2(), tuple.getT1()));
    }

    public Mono<RecordVO> getById(String id) {
        return Mono.fromFuture(recordByIdCache.get(id))
                .flatMap(opt -> opt
                        .map(Mono::just)
                        .orElseGet(() -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "发送记录不存在"))));
    }

    public void create(RecordEntity record) {
        List<RecordEntity> toFlush = null;
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
        List<RecordEntity> toFlush = null;
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
        List<RecordEntity> toFlush;
        synchronized (buffer) {
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        if (toFlush != null && !toFlush.isEmpty()) {
            recordRepository.saveAll(toFlush)
                    .doOnError(e -> log.error("SEND_RECORD_BATCH_SAVE_FAILED on shutdown size={} error={}", toFlush.size(), e.getMessage()))
                    .block();
        }
    }

    private void flushAsync(List<RecordEntity> batch) {
        recordRepository.saveAll(batch)
                .doOnError(e -> log.error("SEND_RECORD_BATCH_SAVE_FAILED size={} error={}", batch.size(), e.getMessage()))
                .subscribe();
    }
}
