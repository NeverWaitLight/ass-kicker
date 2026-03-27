package com.github.waitlight.asskicker.channel;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChannelFactory {

    public ChannelHandler create(ChannelProviderEntity provider) {
        Assert.notNull(provider, "ChannelProviderEntity must not be null");

        try {
            return switch (provider.getProviderType()) {
                case APNS -> new ApnsChannelHandler(provider);
                case FCM -> new FcmChannelHandler(provider);
                default -> {
                    log.warn("Unsupported channel provider type: {}", provider.getProviderType());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("Failed to create ChannelHandler for channel {}: {}", provider.getCode(), e.getMessage(), e);
            return null;
        }
    }

    public List<ChannelProviderType> getSupportedTypes() {
        return List.of(ChannelProviderType.APNS, ChannelProviderType.FCM);
    }
}
