package com.github.waitlight.asskicker.channels;

import lombok.Data;

@Data
public class MsgResp {

    private boolean success;
    private String messageId;
    private String errorCode;
    private String errorMessage;

    public static MsgResp success() {
        MsgResp response = new MsgResp();
        response.success = true;
        return response;
    }

    public static MsgResp success(String messageId) {
        MsgResp response = new MsgResp();
        response.success = true;
        response.messageId = messageId;
        return response;
    }

    public static MsgResp failure(String errorCode, String errorMessage) {
        MsgResp response = new MsgResp();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        return response;
    }
}