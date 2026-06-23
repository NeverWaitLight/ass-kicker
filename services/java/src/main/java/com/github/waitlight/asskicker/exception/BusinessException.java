package com.github.waitlight.asskicker.exception;

import lombok.Getter;

/**
 * 基础业务异常，所有业务异常都继承此类
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final String messageKey;
    private final Object[] args;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.messageKey = null;
        this.args = null;
    }

    public BusinessException(String code, String messageKey, Object[] args) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.args = args;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.messageKey = null;
        this.args = null;
    }
}