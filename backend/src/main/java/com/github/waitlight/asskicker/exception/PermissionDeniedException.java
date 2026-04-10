package com.github.waitlight.asskicker.exception;

/**
 * 权限拒绝异常
 */
public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException() {
        super("PERMISSION_DENIED", "No permission to perform this operation");
    }

    public PermissionDeniedException(String message) {
        super("PERMISSION_DENIED", message);
    }

    public PermissionDeniedException(String messageKey, Object... args) {
        super("PERMISSION_DENIED", messageKey, args);
    }
}