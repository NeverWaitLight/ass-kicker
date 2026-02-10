package com.github.waitlight.asskicker.model;

public enum Language {
    ZH_HANS("zh-Hans", "简体中文"),
    ZH_HANT("zh-Hant", "繁体中文"),
    EN("en", "English"),
    FR("fr", "Français"),
    DE("de", "Deutsch");

    private final String code;
    private final String displayName;

    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unknown language code: " + code);
    }
}