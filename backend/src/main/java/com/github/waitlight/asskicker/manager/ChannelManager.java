package com.github.waitlight.asskicker.manager;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.model.ChannelConfig;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelConfigService;
import com.github.waitlight.asskicker.service.TemplateService;
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

    private final TemplateService templateService;
    private final ChannelConfigService channelConfigService;
    private final ChannelFactory channelFactory;

    private final ConcurrentHashMap<String, Channel<?>> channelCache = new ConcurrentHashMap<>();

    public Mono<ChannelConfig> selectChannel(String templateCode) {
        return templateService.findByCode(templateCode)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateCode)))
                .flatMap(template -> {
                    List<ChannelType> types = template.getApplicableChannelTypes();
                    if (types == null || types.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Template has no applicable channel types: " + templateCode));
                    }
                    return channelConfigService.findByTypes(types)
                            .collectList()
                            .flatMap(channels -> {
                                if (channels.isEmpty()) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                            "No available channel for types: " + types));
                                }
                                ChannelConfig selected = channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
                                return Mono.just(selected);
                            });
                });
    }

    public Channel<?> resolveChannel(ChannelConfig channelConfig) {
        return channelCache.computeIfAbsent(channelConfig.getId(), id -> channelFactory.create(channelConfig));
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
