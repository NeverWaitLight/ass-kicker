package com.github.waitlight.asskicker.channels.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信通道，直接发送完整内容（单变量模板）。
 */
public class AliyunSmsChannel extends SmsChannel<AliyunSmsChannelConfig> {

    public AliyunSmsChannel(AliyunSmsChannelConfig config) {
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
            Config apiConfig = new Config()
                    .setAccessKeyId(config.getAccessKeyId())
                    .setAccessKeySecret(config.getAccessKeySecret())
                    .setRegionId(config.getRegionId())
                    .setEndpoint("dysmsapi.aliyuncs.com");
            apiConfig.setReadTimeout((int) config.getTimeout().toMillis());
            apiConfig.setConnectTimeout((int) Math.min(10000, config.getTimeout().toMillis()));
            Client client = new Client(apiConfig);

            String templateParamKey = config.getTemplateParamKey() != null && !config.getTemplateParamKey().isBlank()
                    ? config.getTemplateParamKey() : "content";
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put(templateParamKey, content);
            String templateParamJson = com.aliyun.teautil.Common.toJSONString(paramMap);

            SendSmsRequest sendReq = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(config.getSignName())
                    .setTemplateCode(config.getTemplateCode())
                    .setTemplateParam(templateParamJson);

            int retries = config.getMaxRetries();
            Exception lastEx = null;
            for (int i = 0; i <= retries; i++) {
                try {
                    SendSmsResponse response = client.sendSms(sendReq);
                    SendSmsResponseBody body = response.getBody();
                    if (body == null) {
                        return MsgResp.failure("ALIYUN_SMS_ERROR", "阿里云返回空响应");
                    }
                    String code = body.getCode();
                    if ("OK".equals(code)) {
                        return MsgResp.success(body.getBizId() != null ? body.getBizId() : "ok");
                    }
                    String errorCode = categorizeAliyunCode(code);
                    return MsgResp.failure(errorCode, body.getMessage() != null ? body.getMessage() : code);
                } catch (Exception e) {
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
            return MsgResp.failure(errorCode, lastEx != null ? lastEx.getMessage() : "阿里云短信发送失败");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private boolean isRetryable(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("Timeout") || msg.contains("Connection") || msg.contains("503") || msg.contains("502");
    }

    private String categorizeAliyunCode(String code) {
        if (code == null) {
            return "ALIYUN_SMS_ERROR";
        }
        if ("isv.BUSINESS_LIMIT_CONTROL".equals(code) || "isv.INVALID_PARAMETERS".equals(code)) {
            return "RATE_LIMIT_EXCEEDED";
        }
        if ("isv.INVALID_ACCESS_KEY_ID".equals(code) || "isv.MOBILE_NUMBER_ILLEGAL".equals(code)) {
            return "AUTHENTICATION_FAILED";
        }
        if (code.startsWith("isv.")) {
            return "INVALID_REQUEST";
        }
        return "ALIYUN_SMS_ERROR";
    }

    private String categorizeError(Exception ex) {
        if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Timeout")) {
            return "TIMEOUT";
        }
        if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Connection")) {
            return "CONNECTION_FAILED";
        }
        return "ALIYUN_SMS_ERROR";
    }
}
