package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;

/**
 * 腾讯云短信通道，直接发送完整内容（单变量模板）。
 */
public class TencentSmsChannel extends SmsChannel<TencentSmsChannelConfig> {

    public TencentSmsChannel(TencentSmsChannelConfig config) {
        super(config);
    }

    @Override
    public MsgResp send(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }
        String phone = request.getRecipient();
        if (phone == null || phone.isBlank()) {
            return MsgResp.failure("INVALID_REQUEST", "手机号不能为空");
        }
        String content = request.getContent() != null ? request.getContent() : "";
        try {
            Credential credential = new Credential(config.getSecretId(), config.getSecretKey());
            SmsClient client = new SmsClient(credential, config.getRegion());

            SendSmsRequest sendReq = new SendSmsRequest();
            sendReq.setPhoneNumberSet(new String[]{phone});
            sendReq.setSmsSdkAppId(config.getSdkAppId());
            sendReq.setSignName(config.getSignName());
            sendReq.setTemplateId(config.getTemplateId());
            sendReq.setTemplateParamSet(new String[]{content});

            int retries = config.getMaxRetries();
            Exception lastEx = null;
            for (int i = 0; i <= retries; i++) {
                try {
                    SendSmsResponse response = client.SendSms(sendReq);
                    SendStatus[] statusSet = response.getSendStatusSet();
                    if (statusSet != null && statusSet.length > 0) {
                        SendStatus status = statusSet[0];
                        String code = status.getCode();
                        if ("Ok".equals(code)) {
                            return MsgResp.success(status.getSerialNo() != null ? status.getSerialNo() : "ok");
                        }
                        String errorCode = categorizeTencentCode(code);
                        return MsgResp.failure(errorCode, status.getMessage() != null ? status.getMessage() : code);
                    }
                    return MsgResp.failure("TENCENT_SMS_ERROR", "腾讯云返回空状态");
                } catch (TencentCloudSDKException e) {
                    lastEx = e;
                    if (i < retries && isRetryable(e)) {
                        try {
                            Thread.sleep(config.getRetryDelay().toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return MsgResp.failure("SEND_INTERRUPTED", ie.getMessage());
                        }
                    } else {
                        break;
                    }
                }
            }
            String errorCode = categorizeError(lastEx);
            return MsgResp.failure(errorCode, lastEx != null ? lastEx.getMessage() : "腾讯云短信发送失败");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private boolean isRetryable(TencentCloudSDKException e) {
        String code = e.getErrorCode();
        if (code == null) {
            return false;
        }
        return "RequestLimitExceeded".equals(code) || "ResourceUnavailable".equals(code)
                || (e.getMessage() != null && (e.getMessage().contains("503") || e.getMessage().contains("502")));
    }

    private String categorizeTencentCode(String code) {
        if (code == null) {
            return "TENCENT_SMS_ERROR";
        }
        if ("LimitExceeded.PhoneNumberDailyLimit".equals(code) || "LimitExceeded".equals(code)) {
            return "RATE_LIMIT_EXCEEDED";
        }
        if ("AuthFailure".equals(code) || "AuthFailure.SignatureFailure".equals(code)) {
            return "AUTHENTICATION_FAILED";
        }
        if ("InvalidParameter".equals(code) || "InvalidParameterValue.IncorrectPhoneNumber".equals(code)) {
            return "INVALID_REQUEST";
        }
        return "TENCENT_SMS_ERROR";
    }

    private String categorizeError(Exception ex) {
        if (ex instanceof TencentCloudSDKException sdkEx) {
            return categorizeTencentCode(sdkEx.getErrorCode());
        }
        if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Timeout")) {
            return "TIMEOUT";
        }
        if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Connection")) {
            return "CONNECTION_FAILED";
        }
        return "TENCENT_SMS_ERROR";
    }
}
