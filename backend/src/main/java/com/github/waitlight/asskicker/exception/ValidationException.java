package com.github.waitlight.asskicker.exception;

/**
 * 验证异常，用于参数校验失败等场景
 */
public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}