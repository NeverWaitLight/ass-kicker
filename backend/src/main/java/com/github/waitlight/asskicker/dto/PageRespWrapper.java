package com.github.waitlight.asskicker.dto;

import java.util.List;

public record PageRespWrapper<T>(
        String code,
        String message,
        int page,
        int size,
        long total,
        List<T> data) {
    public static <T> PageRespWrapper<T> success(int page, int size, long total, List<T> data) {
        return new PageRespWrapper<>("200", "success", page, size, total, data);
    }

    public static <T> PageRespWrapper<T> error(String code, String message) {
        return new PageRespWrapper<>(code, message, 0, 0, 0, null);
    }
}
