package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ChannelType {
    SMS,
    EMAIL,
    IM,
    PUSH;

    @JsonCreator
    public static ChannelType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return ChannelType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("通道类型必须为SMS、EMAIL、IM、PUSH");
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
