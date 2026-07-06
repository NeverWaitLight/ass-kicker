package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;
import com.github.waitlight.asskicker.dto.channel.ChannelDebugResultVO;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelDebugService {

    private final ChannelService channelService;
    private final ChannelFactory channelFactory;
    @Qualifier(ChannelObjectMapperConfig.BEAN_NAME)
    private final ObjectMapper channelObjectMapper;

    public Mono<ChannelDebugResultVO> send(String channelId, Map<String, Object> requestPayload) {
        return channelService.getById(channelId)
                .flatMap(entity -> executeSend(entity, requestPayload))
                .onErrorResume(NotFoundException.class, e -> {
                    log.warn("Channel not found: {}", channelId);
                    return Mono.just(ChannelDebugResultVO.error("Channel not found: " + channelId));
                })
                .onErrorResume(e -> {
                    log.error("Send failed for channel {}", channelId, e);
                    return Mono.just(ChannelDebugResultVO.error(e.getMessage()));
                });
    }

    private Mono<ChannelDebugResultVO> executeSend(ChannelEntity entity, Map<String, Object> requestPayload) {
        AbstractChannel<?> channel = channelFactory.create(entity);
        if (channel == null) {
            return Mono.just(ChannelDebugResultVO.error(
                    "Failed to create channel instance for type=" + entity.getType()
                            + ", provider=" + entity.getProvider()));
        }
        return sendWithChannel(channel, requestPayload)
                .map(ChannelDebugResultVO::success)
                .doFinally(signal -> dispose(channel));
    }

    @SuppressWarnings("unchecked")
    private <T extends SendReq> Mono<String> sendWithChannel(AbstractChannel<T> channel,
            Map<String, Object> requestPayload) {
        Channel spec = AnnotationUtils.findAnnotation(channel.getClass(), Channel.class);
        if (spec == null) {
            return Mono.error(new IllegalArgumentException(
                    "Channel " + channel.getClass().getName() + " missing @Channel annotation"));
        }
        try {
            T sendReq = (T) channelObjectMapper.convertValue(requestPayload, spec.reqType());
            return channel.send(sendReq);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid request payload: " + e.getMessage(), e));
        }
    }

    private void dispose(AbstractChannel<?> channel) {
        try {
            channel.dispose();
            log.debug("Channel instance disposed for code={}", channel.getCode());
        } catch (Exception e) {
            log.warn("Failed to dispose channel instance", e);
        }
    }
}
