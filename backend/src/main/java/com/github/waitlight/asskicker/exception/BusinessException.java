package com.github.waitlight.asskicker.exception;

import lombok.Getter;

/**
 * 基础业务异常，所有业务异常都继承此类
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}