package com.github.waitlight.asskicker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.channel.impl.EmailReq;
import com.github.waitlight.asskicker.channel.impl.ImReq;
import com.github.waitlight.asskicker.channel.impl.PushReq;
import com.github.waitlight.asskicker.channel.impl.SmsReq;
import com.github.waitlight.asskicker.dto.channel.ChannelDebugResultVO;
import com.github.waitlight.asskicker.exception.NotFoundException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.github.waitlight.asskicker.config.ChannelObjectMapperConfig;

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
        Channel<?> channel = null;
        try {
            channel = channelFactory.create(entity);
            if (channel == null) {
                return Mono.just(ChannelDebugResultVO.error(
                        "Failed to create channel instance for type=" + entity.getType() +
                        ", provider=" + entity.getProvider()));
            }

            Channel<?> finalChannel = channel;
            return sendWithChannel(finalChannel, requestPayload)
                    .map(ChannelDebugResultVO::success)
                    .doFinally(signal -> {
                        try {
                            finalChannel.dispose();
                            log.debug("Channel instance disposed for code={}", finalChannel.getCode());
                        } catch (Exception e) {
                            log.warn("Failed to dispose channel instance", e);
                        }
                    });
        } catch (Exception e) {
            if (channel != null) {
                try {
                    channel.dispose();
                } catch (Exception disposeEx) {
                    log.warn("Failed to dispose channel after error", disposeEx);
                }
            }
            return Mono.just(ChannelDebugResultVO.error("Failed to execute send: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SendReq> Mono<String> sendWithChannel(Channel<T> channel, Map<String, Object> requestPayload) {
        try {
            Class<? extends SendReq> reqClass = determineSendReqClass(channel);
            T sendReq = (T) channelObjectMapper.convertValue(requestPayload, reqClass);
            return channel.send(sendReq);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid request payload: " + e.getMessage(), e));
        }
    }

    private Class<? extends SendReq> determineSendReqClass(Channel<?> channel) {
        String className = channel.getClass().getName();

        if (className.contains("SmtpEmailChannel")) {
            return EmailReq.class;
        } else if (className.contains("AliyunSmsChannel") || className.contains("TencentSmsChannel")) {
            return SmsReq.class;
        } else if (className.contains("ApnsPushChannel") || className.contains("FcmPushChannel")) {
            return PushReq.class;
        } else if (className.contains("DingTalkImChannel") || className.contains("FeishuImChannel")) {
            return ImReq.class;
        }

        throw new IllegalArgumentException("Unknown channel type: " + className);
    }
}
