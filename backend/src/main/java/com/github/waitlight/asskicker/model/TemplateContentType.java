package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum TemplateContentType {
    PLAIN_TEXT,
    HTML,
    JSON;

    @JsonCreator
    public static TemplateContentType fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return TemplateContentType.valueOf(normalized.toUpperCase(Locale.ROOT).replace("-", "_"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("模板内容类型必须为PLAIN_TEXT、HTML、JSON");
        }
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
