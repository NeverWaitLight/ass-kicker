package com.github.waitlight.asskicker.channel.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Channel(type = ChannelType.SMS, provider = ChannelProvider.TENCENT)
public class TencentSmsChannel extends AbstractChannel<SmsReq> {

    private static final String DEFAULT_ENDPOINT = "sms.tencentcloudapi.com";
    private static final String DEFAULT_REGION = "ap-guangzhou";
    private static final String SUCCESS_CODE = "Ok";

    private final SmsClient client;
    private final String smsSdkAppId;

    public TencentSmsChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        super(entity, webClient, objectMapper);
        Properties properties = objectMapper.convertValue(entity.getProperties(), Properties.class);
        try {
            Credential credential = new Credential(properties.getSecretId(), properties.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(StringUtils.defaultIfBlank(properties.getEndpoint(), DEFAULT_ENDPOINT));
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            this.client = new SmsClient(credential,
                    StringUtils.defaultIfBlank(properties.getRegion(), DEFAULT_REGION),
                    clientProfile);
            this.smsSdkAppId = properties.getSmsSdkAppId();
        } catch (Exception e) {
            throw new IllegalStateException("TENCENT_SMS SDK client init failed", e);
        }
    }

    @Override
    public Mono<String> send(SmsReq req) {
        try {
            SendSmsRequest sendSmsRequest = new SendSmsRequest();
            sendSmsRequest.setSmsSdkAppId(smsSdkAppId);
            sendSmsRequest.setSignName(req.getSignName());
            sendSmsRequest.setTemplateId(req.getTemplateId());
            sendSmsRequest.setPhoneNumberSet(new String[]{buildE164(req.getCountryCode(), req.getPhoneNumber())});
            if (req.getTemplateParam() != null && !req.getTemplateParam().isEmpty()) {
                sendSmsRequest.setTemplateParamSet(req.getTemplateParam().values().toArray(new String[0]));
            }

            SendSmsResponse resp = client.SendSms(sendSmsRequest);
            SendStatus[] statuses = resp.getSendStatusSet();
            if (statuses == null || statuses.length == 0) {
                return Mono.error(new SendException("TENCENT_SMS empty SendStatusSet"));
            }
            SendStatus status = statuses[0];
            if (!StringUtils.equalsIgnoreCase(SUCCESS_CODE, status.getCode())) {
                return Mono.error(new SendException(status.getCode() + ": " + status.getMessage()));
            }
        } catch (Exception e) {
            throw new SendException(e.getMessage());
        }
        return Mono.empty();
    }

    private static String buildE164(String countryCode, String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return phoneNumber;
        }
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        }
        if (StringUtils.isBlank(countryCode)) {
            return phoneNumber;
        }
        String cc = countryCode.startsWith("+") ? countryCode.substring(1) : countryCode;
        return "+" + cc + phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        /** 腾讯云 API 密钥 SecretId */
        @NotBlank
        private String secretId;

        /** 腾讯云 API 密钥 SecretKey */
        @NotBlank
        private String secretKey;

        /** 短信应用 SDK AppID,在腾讯云短信控制台创建应用后获取 */
        @NotBlank
        private String smsSdkAppId;

        /** 服务地域,如 ap-guangzhou;留空使用默认 ap-guangzhou */
        private String region;

        /** 短信服务 endpoint,留空使用默认值 sms.tencentcloudapi.com,可写 host 或 host:port */
        @Pattern(regexp = "^$|^[A-Za-z0-9.-]+(:\\d+)?$")
        private String endpoint;
    }
}
