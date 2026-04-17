package com.github.waitlight.asskicker.dto;

public record Resp<T>(String code, String message, T data) {
    public static <T> Resp<T> success(T data) {
        return new Resp<>("200", "success", data);
    }

    public static <T> Resp<T> error(String code, String message) {
        return new Resp<>(code, message, null);
    }
}
