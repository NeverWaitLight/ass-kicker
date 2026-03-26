package com.github.waitlight.asskicker.channel;

import java.util.List;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChannelFactory {

    public ChannelHandler create(ChannelProviderEntity entity) {
        if (entity == null || entity.getProviderType() == null) {
            return null;
        }
        try {
            return switch (entity.getProviderType()) {
                case APNS -> new ApnsChannelHandler(entity);
                case FCM -> new FcmChannelHandler(entity);
                default -> {
                    log.warn("Unsupported channel provider type: {}", entity.getProviderType());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Failed to create ChannelHandler for channel {}: {}", entity.getCode(), e.getMessage(), e);
            return null;
        }
    }

    public List<ChannelProviderType> getSupportedTypes() {
        return List.of(ChannelProviderType.APNS, ChannelProviderType.FCM);
    }
}
