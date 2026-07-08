package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelManager {

    private static final Comparator<AbstractChannel<?>> BY_CODE = Comparator
            .comparing(AbstractChannel::getCode);

    private final ChannelService channelService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, AbstractChannel<?>> cache = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile boolean closed = false;

    @PostConstruct
    void init() {
        List<ChannelEntity> enabled = channelService.findEnabled().collectList().block();
        if (enabled == null) {
            enabled = List.of();
        }
        int loaded = 0;
        for (ChannelEntity entity : enabled) {
            AbstractChannel<?> channel = channelFactory.create(entity);
            if (channel == null) {
                log.warn("Skip channel {}, channel creation returned null", entity.getCode());
                continue;
            }
            cache.put(entity.getId(), channel);
            loaded++;
        }
        log.info("Loaded {} channel channel(s)", loaded);
    }

    public Mono<AbstractChannel<?>> chose(ChannelType channelType, String recipient) {
        List<AbstractChannel<?>> matching = cache.values().stream()
                .filter(c -> c.getType() == channelType)
                .sorted(BY_CODE)
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        AbstractChannel<?> chosen = matching.get(0);
        log.debug("Selected channel {} for recipient {}", chosen.getCode(), recipient);
        return Mono.just(chosen);
    }

    public Mono<AbstractChannel<?>> chose(SendReq req) {
        return chose(req.getType(), req.getProvider());
    }

    public Mono<AbstractChannel<?>> chose(ChannelType channelType, ChannelProvider provider) {
        AbstractChannel<?> chosen = cache.values().stream()
                .filter(c -> channelType == null || c.getType() == channelType)
                .filter(c -> provider == null || c.getProvider() == provider)
                .min(BY_CODE)
                .orElse(null);
        return chosen == null ? Mono.empty() : Mono.just(chosen);
    }

    public void refresh() {
        refreshLock.lock();
        try {
            if (closed) {
                log.info("Channel cache refresh skipped, manager already closed");
                return;
            }
            ConcurrentHashMap<String, AbstractChannel<?>> next = new ConcurrentHashMap<>();
            List<ChannelEntity> enabledProvider = channelService.findEnabled().collectList().block();
            if (enabledProvider == null) {
                enabledProvider = List.of();
            }

            for (ChannelEntity entity : enabledProvider) {
                AbstractChannel<?> channel = channelFactory.create(entity);
                if (channel != null) {
                    next.put(entity.getId(), channel);
                }
            }
            List<AbstractChannel<?>> previous = new ArrayList<>(cache.values());
            cache.clear();
            cache.putAll(next);
            log.info("Refreshed channel cache, {} channel(s)", next.size());
            disposeAll(previous);
        } catch (Exception e) {
            log.error("Channel cache refresh failed, keeping previous cache", e);
        } finally {
            refreshLock.unlock();
        }
    }

    @PreDestroy
    void shutdown() {
        refreshLock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            List<AbstractChannel<?>> toDispose = new ArrayList<>(cache.values());
            cache.clear();
            log.info("Shutting down ChannelManager, disposing {} channel(s)", toDispose.size());
            disposeAll(toDispose);
            log.info("ChannelManager shutdown complete");
        } finally {
            refreshLock.unlock();
        }
    }

    private void disposeAll(List<AbstractChannel<?>> channels) {
        for (AbstractChannel<?> c : channels) {
            try {
                c.dispose();
            } catch (Exception e) {
                log.warn("Channel {} dispose failed", c.getCode(), e);
            }
        }
    }

    public int getChannelCount() {
        return cache.size();
    }
}
