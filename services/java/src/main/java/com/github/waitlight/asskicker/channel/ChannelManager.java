package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
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

    private static final Comparator<Channel<?>> BY_CODE = Comparator
            .comparing(Channel::getCode);

    private final ChannelService channelService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, Channel<?>> cache = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    @PostConstruct
    void init() {
        List<ChannelEntity> enabled = channelService.findEnabled().collectList().block();
        if (enabled == null) {
            enabled = List.of();
        }
        int loaded = 0;
        for (ChannelEntity entity : enabled) {
            Channel<?> channel = channelFactory.create(entity);
            if (channel == null) {
                log.warn("Skip channel {}, channel creation returned null", entity.getCode());
                continue;
            }
            cache.put(entity.getId(), channel);
            loaded++;
        }
        log.info("Loaded {} channel channel(s)", loaded);
    }

    public Mono<Channel<?>> chose(ChannelType channelType, String recipient) {
        List<Channel<?>> matching = cache.values().stream()
                .filter(c -> c.getChannelType() == channelType)
                .filter(Channel::isEnabled)
                .sorted(BY_CODE)
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        Channel<?> chosen = matching.get(0);
        log.debug("Selected channel {} for recipient {}", chosen.getCode(), recipient);
        return Mono.just(chosen);
    }

    public void refresh() {
        refreshLock.lock();
        try {
            ConcurrentHashMap<String, Channel<?>> next = new ConcurrentHashMap<>();
            List<ChannelEntity> enabledProvider = channelService.findEnabled().collectList().block();
            if (enabledProvider == null) {
                enabledProvider = List.of();
            }

            for (ChannelEntity entity : enabledProvider) {
                Channel<?> channel = channelFactory.create(entity);
                if (channel != null) {
                    next.put(entity.getId(), channel);
                }
            }
            List<Channel<?>> previous = new ArrayList<>(cache.values());
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
        disposeAll(new ArrayList<>(cache.values()));
        cache.clear();
    }

    private void disposeAll(List<Channel<?>> channels) {
        for (Channel<?> c : channels) {
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
