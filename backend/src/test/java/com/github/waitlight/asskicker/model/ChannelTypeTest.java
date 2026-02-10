package com.github.waitlight.asskicker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChannelTypeTest {

    @Test
    void parsesValidValues() {
        assertEquals(ChannelType.SMS, ChannelType.fromString("SMS"));
        assertEquals(ChannelType.EMAIL, ChannelType.fromString("email"));
        assertEquals(ChannelType.IM, ChannelType.fromString(" Im "));
        assertEquals(ChannelType.PUSH, ChannelType.fromString("push"));
    }

    @Test
    void rejectsInvalidValues() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ChannelType.fromString("invalid"));
        assertEquals("渠道类型必须为SMS、EMAIL、IM、PUSH", ex.getMessage());
    }
}
