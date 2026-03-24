package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelEntityService;
import com.github.waitlight.asskicker.util.RecipientsRuleMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChannelManager implements DisposableBean {

    private final ChannelEntityService channelEntityService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, Channel<?>> channelCache = new ConcurrentHashMap<>();

    /**
     * Select a channel: by type, then by include/exclude recipient rules, then random among matches.
     * Returns the cached {@link Channel} instance used for sending.
     */
    public Mono<Channel<?>> selectChannel(ChannelType channelType, String recipient) {
        if (channelType == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel type is required"));
        }
        return channelEntityService.findByTypes(List.of(channelType))
                .collectList()
                .flatMap(channels -> {
                    if (channels.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No available channel for type: " + channelType));
                    }
                    List<ChannelEntity> matched = channels.stream()
                            .filter(ch -> RecipientsRuleMatcher.isAllowed(recipient,
                                    ch.getIncludeRecipientRegex(), ch.getExcludeRecipientRegex()))
                            .toList();
                    if (matched.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "No channel matches recipient rules for type: " + channelType));
                    }
                    ChannelEntity selected = matched.get(ThreadLocalRandom.current().nextInt(matched.size()));
                    return Mono.just(resolveChannel(selected));
                });
    }

    public Channel<?> resolveChannel(ChannelEntity channelEntity) {
        String id = channelEntity.getId();
        return channelCache.computeIfAbsent(id, i -> channelFactory.create(channelEntity));
    }

    @Override
    public void destroy() {
        channelCache.forEach((id, channel) -> {
            if (channel instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ex) {
                    log.warn("SEND_CHANNEL_CLOSE_FAILED channelId={} errorCode={} errorMessage={}",
                            id, "CHANNEL_CLOSE_FAILED",
                            ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage()
                                    : "Failed to close channel");
                }
            }
        });
        channelCache.clear();
    }
}
