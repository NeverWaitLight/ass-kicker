package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    ZH_CN("zh-CN", "简体中文"),
    ZH_TW("zh-TW", "繁体中文"),
    EN("en", "English"),
    FR("fr", "Français"),
    DE("de", "Deutsch");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @JsonCreator
    public static Language fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(normalized)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unknown language code: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
