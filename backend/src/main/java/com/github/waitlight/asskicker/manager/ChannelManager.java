package com.github.waitlight.asskicker.manager;

import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Component
public class ChannelManager {

    private final TemplateService templateService;
    private final ChannelService channelService;

    public Mono<Channel> selectChannel(String templateCode) {
        return templateService.findByCode(templateCode)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateCode)))
                .flatMap(template -> {
                    List<ChannelType> types = template.getApplicableChannelTypes();
                    if (types == null || types.isEmpty()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Template has no applicable channel types: " + templateCode));
                    }
                    return channelService.findByTypes(types)
                            .collectList()
                            .flatMap(channels -> {
                                if (channels.isEmpty()) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                            "No available channel for types: " + types));
                                }
                                Channel selected = channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
                                return Mono.just(selected);
                            });
                });
    }
}
