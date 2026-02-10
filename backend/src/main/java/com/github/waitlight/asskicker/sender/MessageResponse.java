package com.github.waitlight.asskicker.sender;

import lombok.Data;

@Data
public class MessageResponse {

    private boolean success;
    private String messageId;
    private String errorCode;
    private String errorMessage;

    public static MessageResponse success() {
        MessageResponse response = new MessageResponse();
        response.success = true;
        return response;
    }

    public static MessageResponse success(String messageId) {
        MessageResponse response = new MessageResponse();
        response.success = true;
        response.messageId = messageId;
        return response;
    }

    public static MessageResponse failure(String errorCode, String errorMessage) {
        MessageResponse response = new MessageResponse();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        return response;
    }
}