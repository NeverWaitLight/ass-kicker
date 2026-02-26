package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum SenderType {
    SMS,
    EMAIL,
    IM,
    PUSH;

    @JsonCreator
    public static SenderType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return SenderType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("发送端类型必须为SMS、EMAIL、IM、PUSH");
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
