package com.github.waitlight.asskicker.dto;

import java.util.List;

public record PageResp<T>(
        String code,
        String message,
        int page,
        int size,
        long total,
        List<T> data) {
    public static <T> PageResp<T> success(int page, int size, long total, List<T> data) {
        return new PageResp<>("200", "success", page, size, total, data);
    }

    public static <T> PageResp<T> error(String code, String message) {
        return new PageResp<>(code, message, 0, 0, 0, null);
    }
}
