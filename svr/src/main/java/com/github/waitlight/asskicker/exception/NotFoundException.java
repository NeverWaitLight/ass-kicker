package com.github.waitlight.asskicker.exception;

/**
 * 资源不存在异常
 */
public class NotFoundException extends BusinessException {
    public NotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found: %s", resource, id));
    }

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public NotFoundException(String messageKey, Object... args) {
        super("RESOURCE_NOT_FOUND", messageKey, args);
    }
}