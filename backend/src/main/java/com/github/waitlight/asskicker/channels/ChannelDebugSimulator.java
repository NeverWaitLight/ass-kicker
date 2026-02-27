package com.github.waitlight.asskicker.channels;

import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ChannelDebugSimulator {

    private static final Logger logger = LoggerFactory.getLogger(ChannelDebugSimulator.class);
    private final ChannelDebugProperties properties;

    public ChannelDebugSimulator(ChannelDebugProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public MsgResp simulate(String channelName) {
        int sleepMs = resolveSleepMillis();
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return MsgResp.failure("SEND_INTERRUPTED", ex.getMessage());
        }
        String messageId = "DEBUG-" + UUID.randomUUID();
        logger.info("CHANNEL_DEBUG_SIMULATED channel={} sleepMs={} messageId={}",
                channelName, sleepMs, messageId);
        return MsgResp.success(messageId);
    }

    private int resolveSleepMillis() {
        int min = properties.getMinSleepMs();
        int max = properties.getMaxSleepMs();
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
