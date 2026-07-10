package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    ZH_CN("zh-CN"),
    ZH_TW("zh-TW"),
    EN("en"),
    FR("fr"),
    DE("de");

    private final String code;

    Language(String code) {
        this.code = code;
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

}
