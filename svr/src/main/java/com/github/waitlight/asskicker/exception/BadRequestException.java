package com.github.waitlight.asskicker.exception;

/**
 * 参数校验失败等场景
 */
public class BadRequestException extends BusinessException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }

    public BadRequestException(String messageKey, Object... args) {
        super("BAD_REQUEST", messageKey, args);
    }
}