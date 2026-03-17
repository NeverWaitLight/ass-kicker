package com.github.waitlight.asskicker.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channels.push.PushChannelFactory;
import com.github.waitlight.asskicker.channels.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.channels.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.ChannelService;
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
    private final ChannelService channelService;
    private final EmailChannelFactory emailChannelFactory;
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelFactory imChannelFactory;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelFactory pushChannelFactory;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelFactory smsChannelFactory;
    private final SmsChannelConfigConverter smsChannelConfigConverter;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, com.github.waitlight.asskicker.channels.Channel<?>> channelCache = new ConcurrentHashMap<>();

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

    public com.github.waitlight.asskicker.channels.Channel<?> resolveChannel(Channel channelEntity) {
        return channelCache.computeIfAbsent(channelEntity.getId(), id -> buildChannel(channelEntity));
    }

    private com.github.waitlight.asskicker.channels.Channel<?> buildChannel(Channel channelEntity) {
        ChannelType type = channelEntity.getType();
        Map<String, Object> properties = readProperties(channelEntity.getPropertiesJson());
        if (type == ChannelType.EMAIL) {
            ChannelConfig config = emailChannelConfigConverter.fromProperties(properties);
            return emailChannelFactory.create(config);
        }
        if (type == ChannelType.IM) {
            ChannelConfig config = imChannelConfigConverter.fromProperties(properties);
            return imChannelFactory.create(config);
        }
        if (type == ChannelType.PUSH) {
            ChannelConfig config = pushChannelConfigConverter.fromProperties(properties);
            return pushChannelFactory.create(config);
        }
        if (type == ChannelType.SMS) {
            ChannelConfig config = smsChannelConfigConverter.fromProperties(properties);
            return smsChannelFactory.create(config);
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
