package com.github.waitlight.asskicker.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channel.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channel.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channel.sms.SmsChannelConfigConverter;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChannelManager implements DisposableBean {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TemplateService templateService;
    private final ChannelConfigService channelConfigService;
    private final ChannelFactory channelFactory;
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelConfigConverter smsChannelConfigConverter;
    private final ObjectMapper objectMapper;

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

    public Channel<?> resolveChannel(ChannelConfig channelConfigEntity) {
        return channelCache.computeIfAbsent(channelConfigEntity.getId(), id -> buildChannel(channelConfigEntity));
    }

    private Channel<?> buildChannel(ChannelConfig channelConfigEntity) {
        ChannelType type = channelConfigEntity.getType();
        Map<String, Object> properties = readProperties(channelConfigEntity.getPropertiesJson());
        if (type == ChannelType.EMAIL) {
            ChannelProperties config = emailChannelConfigConverter.fromProperties(properties);
            return channelFactory.create(config);
        }
        if (type == ChannelType.IM) {
            ChannelProperties config = imChannelConfigConverter.fromProperties(properties);
            return channelFactory.create(config);
        }
        if (type == ChannelType.PUSH) {
            ChannelProperties config = pushChannelConfigConverter.fromProperties(properties);
            return channelFactory.create(config);
        }
        if (type == ChannelType.SMS) {
            ChannelProperties config = smsChannelConfigConverter.fromProperties(properties);
            return channelFactory.create(config);
        }
        return null;
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("SEND_CHANNEL_PROPERTIES_PARSE_FAILED errorCode={} errorMessage={}",
                    "CHANNEL_PROPERTIES_PARSE_FAILED",
                    ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage()
                            : "Failed to parse channel properties");
            return new LinkedHashMap<>();
        }
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
