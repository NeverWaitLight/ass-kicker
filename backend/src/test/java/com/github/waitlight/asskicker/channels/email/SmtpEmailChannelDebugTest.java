package com.github.waitlight.asskicker.channels.email;

import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpEmailChannelDebugTest {

    @Test
    void shouldBypassRealSendWhenDebugEnabled() {
        SmtpEmailChannelConfig config = new SmtpEmailChannelConfig();
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setEnabled(true);
        properties.setMinSleepMs(0);
        properties.setMaxSleepMs(0);

        SmtpEmailChannel channel = new SmtpEmailChannel(config, new ChannelDebugSimulator(properties));
        MsgReq request = MsgReq.builder()
                .recipient("user@example.com")
                .subject("debug")
                .content("hello")
                .build();

        MsgResp response = channel.send(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessageId().startsWith("DEBUG-"));
    }

    @Test
    void shouldKeepRequestValidationInDebugMode() {
        SmtpEmailChannelConfig config = new SmtpEmailChannelConfig();
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setEnabled(true);
        properties.setMinSleepMs(0);
        properties.setMaxSleepMs(0);

        SmtpEmailChannel channel = new SmtpEmailChannel(config, new ChannelDebugSimulator(properties));
        MsgResp response = channel.send(null);

        assertFalse(response.isSuccess());
        assertEquals("INVALID_REQUEST", response.getErrorCode());
    }
}
