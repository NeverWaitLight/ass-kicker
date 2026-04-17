package com.github.waitlight.asskicker.exception;

/**
 * 未认证或凭证无效（HTTP 401）
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String messageKey, Object... args) {
        super("UNAUTHORIZED", messageKey, args);
    }
}
