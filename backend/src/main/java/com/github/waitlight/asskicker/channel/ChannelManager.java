package com.github.waitlight.asskicker.channel;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelProviderService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelManager {

    private static final Comparator<Channel> BY_CODE = Comparator
            .comparing(Channel::getCode);

    private final ChannelProviderService channelProviderService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, Channel> cache = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    @PostConstruct
    void init() {
        List<ChannelProviderEntity> enabled = channelProviderService.findEnabled().collectList().block();
        if (enabled == null) {
            enabled = List.of();
        }
        int loaded = 0;
        for (ChannelProviderEntity entity : enabled) {
            Channel channel = channelFactory.create(entity);
            if (channel == null) {
                log.warn("Skip channel {}, channel creation returned null", entity.getCode());
                continue;
            }
            cache.put(entity.getId(), channel);
            loaded++;
        }
        log.info("Loaded {} channel channel(s)", loaded);
    }

    public Mono<Channel> chose(ChannelType channelType, String recipient) {
        List<Channel> matching = cache.values().stream()
                .filter(c -> c.getChannelType() == channelType)
                .filter(Channel::isEnabled)
                .filter(c -> !c.matchesExclude(recipient))
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        List<Channel> priorityMatches = matching.stream()
                .filter(c -> c.matchesPriority(recipient))
                .sorted(BY_CODE)
                .toList();
        List<Channel> candidates = priorityMatches.isEmpty()
                ? matching.stream().sorted(BY_CODE).toList()
                : priorityMatches;
        Channel chosen = candidates.get(0);
        log.debug("Selected channel {} for recipient {}", chosen.getCode(), recipient);
        return Mono.just(chosen);
    }

    public void refresh() {
        refreshLock.lock();
        try {
            ConcurrentHashMap<String, Channel> next = new ConcurrentHashMap<>();
            List<ChannelProviderEntity> enabledProvider = channelProviderService.findEnabled().collectList().block();
            if (enabledProvider == null) {
                enabledProvider = List.of();
            }

            for (ChannelProviderEntity provider : enabledProvider) {
                Channel channel = channelFactory.create(provider);
                if (channel != null) {
                    next.put(provider.getId(), channel);
                }
            }
            cache.clear();
            cache.putAll(next);
            log.info("Refreshed channel cache, {} channel(s)", next.size());
        } catch (Exception e) {
            log.error("Channel cache refresh failed, keeping previous cache", e);
        } finally {
            refreshLock.unlock();
        }
    }

    public Channel getChannelById(String id) {
        return cache.get(id);
    }

    public List<Channel> getAllChannels() {
        return cache.values().stream().toList();
    }

    public int getChannelCount() {
        return cache.size();
    }
}
