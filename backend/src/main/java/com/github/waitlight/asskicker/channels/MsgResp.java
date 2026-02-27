package com.github.waitlight.asskicker.channels;

import lombok.Data;

/**
 * 通道发送结果，与 MsgReq 对应。各通道 send 返回本类，成功时填 messageId，失败时填 errorCode、errorMessage。
 */
@Data
public class MsgResp {

    /** 是否发送成功。 */
    private boolean success;

    /** 成功时由通道返回的消息 ID（如短信 BizId、邮件 MessageID、APNs apns-id 等），可选。 */
    private String messageId;

    /** 失败时的分类错误码，如 INVALID_REQUEST、RATE_LIMIT_EXCEEDED、TIMEOUT，由各通道约定。 */
    private String errorCode;

    /** 失败时的详细说明，通常为上游或异常信息。 */
    private String errorMessage;

    /** 构造成功结果，无 messageId。 */
    public static MsgResp success() {
        MsgResp response = new MsgResp();
        response.success = true;
        return response;
    }

    /** 构造成功结果，带 messageId。 */
    public static MsgResp success(String messageId) {
        MsgResp response = new MsgResp();
        response.success = true;
        response.messageId = messageId;
        return response;
    }

    /** 构造失败结果。 */
    public static MsgResp failure(String errorCode, String errorMessage) {
        MsgResp response = new MsgResp();
        response.success = false;
        response.errorCode = errorCode;
        response.errorMessage = errorMessage;
        return response;
    }
}