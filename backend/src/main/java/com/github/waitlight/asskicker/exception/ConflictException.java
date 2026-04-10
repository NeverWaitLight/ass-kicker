package com.github.waitlight.asskicker.exception;

/**
 * 资源冲突异常，用于重复创建、并发冲突等场景
 */
public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}