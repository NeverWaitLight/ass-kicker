package com.github.waitlight.asskicker.channel;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelProviderService;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelManager {

    private static final Comparator<ChannelHandlerWrapper> BY_CODE = Comparator
            .comparing(ChannelHandlerWrapper::getCode);

    private final ChannelProviderService channelProviderService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, ChannelHandlerWrapper> cache = new ConcurrentHashMap<>();
    private final ReentrantLock refreshLock = new ReentrantLock();

    @PostConstruct
    void init() {
        List<ChannelProviderEntity> enabled = channelProviderService.findEnabled().collectList().block();
        if (enabled == null) {
            enabled = List.of();
        }
        int loaded = 0;
        for (ChannelProviderEntity entity : enabled) {
            ChannelHandler handler = channelFactory.create(entity);
            if (handler == null) {
                log.warn("Skip channel {}, handler creation returned null", entity.getCode());
                continue;
            }
            ChannelHandlerWrapper wrapper = new ChannelHandlerWrapper(entity, handler);
            cache.put(entity.getId(), wrapper);
            loaded++;
        }
        log.info("Loaded {} channel handler(s)", loaded);
    }

    public Mono<ChannelHandler> selectHandler(ChannelType channelType, String targetAddress) {
        String addr = targetAddress == null ? "" : targetAddress;
        List<ChannelHandlerWrapper> matching = cache.values().stream()
                .filter(w -> w.getChannelType() == channelType)
                .filter(ChannelHandlerWrapper::isEnabled)
                .filter(w -> !w.matchesExclude(addr))
                .toList();
        if (matching.isEmpty()) {
            return Mono.empty();
        }
        List<ChannelHandlerWrapper> priorityMatches = matching.stream()
                .filter(w -> w.matchesPriority(addr))
                .sorted(BY_CODE)
                .toList();
        List<ChannelHandlerWrapper> candidates = priorityMatches.isEmpty()
                ? matching.stream().sorted(BY_CODE).toList()
                : priorityMatches;
        ChannelHandlerWrapper chosen = candidates.get(0);
        log.debug("Selected channel {} for target address {}", chosen.getCode(), targetAddress);
        return Mono.just(chosen.getHandler());
    }

    public void refresh() {
        refreshLock.lock();
        try {
            ConcurrentHashMap<String, ChannelHandlerWrapper> next = new ConcurrentHashMap<>();
            List<ChannelProviderEntity> enabledProvider = channelProviderService.findEnabled().collectList().block();
            if (enabledProvider == null) {
                enabledProvider = List.of();
            }

            for (ChannelProviderEntity provider : enabledProvider) {
                ChannelHandler handler = channelFactory.create(provider);
                ChannelHandlerWrapper wrapper = new ChannelHandlerWrapper(provider, handler);
                next.put(provider.getId(), wrapper);
            }
            cache.clear();
            cache.putAll(next);
            log.info("Refreshed channel cache, {} handler(s)", next.size());
        } catch (Exception e) {
            log.error("Channel cache refresh failed, keeping previous cache", e);
        } finally {
            refreshLock.unlock();
        }
    }

    public ChannelHandler getHandlerById(String id) {
        ChannelHandlerWrapper w = cache.get(id);
        return w == null ? null : w.getHandler();
    }

    public List<ChannelHandler> getAllHandlers() {
        return cache.values().stream().map(ChannelHandlerWrapper::getHandler).toList();
    }

    public int getHandlerCount() {
        return cache.size();
    }

    @Getter
    static final class ChannelHandlerWrapper {

        private final String id;
        private final String code;
        private final ChannelType channelType;
        private final boolean enabled;
        private final Pattern priorityPattern;
        private final Pattern excludePattern;
        private final ChannelHandler handler;

        ChannelHandlerWrapper(ChannelProviderEntity entity, ChannelHandler handler) {
            this.id = entity.getId();
            this.code = entity.getCode();
            this.channelType = entity.getChannelType();
            this.enabled = entity.isEnabled();
            this.handler = handler;
            this.priorityPattern = compilePattern(entity.getCode(), entity.getPriorityAddressRegex(),
                    "priorityAddressRegex");
            this.excludePattern = compilePattern(entity.getCode(), entity.getExcludeAddressRegex(),
                    "excludeAddressRegex");
        }

        private static Pattern compilePattern(String channelCode, String regex, String fieldName) {
            if (StringUtils.isBlank(regex)) {
                return null;
            }
            try {
                return Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                log.error("Invalid {} for channel {}: {}", fieldName, channelCode, regex, e);
                return null;
            }
        }

        boolean matchesPriority(String address) {
            String a = address == null ? "" : address;
            return priorityPattern != null && priorityPattern.matcher(a).matches();
        }

        boolean matchesExclude(String address) {
            String a = address == null ? "" : address;
            return excludePattern != null && excludePattern.matcher(a).matches();
        }
    }
}
