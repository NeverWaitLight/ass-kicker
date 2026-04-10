package com.github.waitlight.asskicker.dto;

public record RespWrapper<T>(String code, String message, T data) {
    public static <T> RespWrapper<T> success(T data) {
        return new RespWrapper<>("200", "success", data);
    }

    public static <T> RespWrapper<T> error(String code, String message) {
        return new RespWrapper<>(code, message, null);
    }
}
