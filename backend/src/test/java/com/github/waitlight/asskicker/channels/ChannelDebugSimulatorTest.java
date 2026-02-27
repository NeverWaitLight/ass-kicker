package com.github.waitlight.asskicker.channels;

import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelDebugSimulatorTest {

    @Test
    void shouldReportEnabledStateAndReturnSimulatedSuccess() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setEnabled(true);
        properties.setMinSleepMs(0);
        properties.setMaxSleepMs(0);
        ChannelDebugSimulator simulator = new ChannelDebugSimulator(properties);

        assertTrue(simulator.isEnabled());
        MsgResp response = simulator.simulate("TestChannel");

        assertTrue(response.isSuccess());
        assertNotNull(response.getMessageId());
        assertTrue(response.getMessageId().startsWith("DEBUG-"));
    }

    @Test
    void shouldReportDisabledState() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setEnabled(false);
        ChannelDebugSimulator simulator = new ChannelDebugSimulator(properties);

        assertFalse(simulator.isEnabled());
    }
}
