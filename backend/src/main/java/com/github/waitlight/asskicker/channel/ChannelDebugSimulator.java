package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ChannelDebugSimulator {

    private final ChannelDebugProperties properties;

    public ChannelDebugSimulator(ChannelDebugProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public MsgResp simulate(String channelName) {
        int sleepMs = properties.getSleepMs();
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return MsgResp.failure("SEND_INTERRUPTED", ex.getMessage());
        }
        String messageId = "DEBUG-" + UUID.randomUUID();
        return MsgResp.success(messageId);
    }
}
