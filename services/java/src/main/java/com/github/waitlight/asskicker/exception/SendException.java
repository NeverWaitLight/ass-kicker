package com.github.waitlight.asskicker.exception;

public class SendException extends RuntimeException {
    public SendException(String message) {
        super(message);
    }
    public SendException(String message, Throwable cause) {}
}
