package com.github.waitlight.asskicker.channel.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Channel(type = ChannelType.SMS, provider = ChannelProvider.ALIYUN, reqType = SmsReq.class)
public class AliyunSmsChannel extends AbstractChannel<SmsReq> {

    private static final String DEFAULT_ENDPOINT = "dysmsapi.aliyuncs.com";

    private final Client client;

    public AliyunSmsChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        super(entity, webClient, objectMapper);
        Properties properties = objectMapper.convertValue(entity.getProperties(), Properties.class);
        try {
            this.client = new Client(new Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret())
                    .setEndpoint(StringUtils.defaultIfBlank(properties.getEndpoint(), DEFAULT_ENDPOINT)));
        } catch (Exception e) {
            throw new IllegalStateException("ALIYUN_SMS SDK client init failed", e);
        }
    }

    @Override
    public Mono<String> send(SmsReq req) {
        try {
            String templateParam = req.getTemplateParam() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(req.getTemplateParam());

            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(req.getPhoneNumber())
                    .setSignName(req.getSignName())
                    .setTemplateCode(req.getTemplateId())
                    .setTemplateParam(templateParam);

            SendSmsResponse resp = client.sendSms(sendSmsRequest);
            if (!StringUtils.equalsIgnoreCase("OK", resp.getBody().getCode())) {
                return Mono.error(new SendException(resp.getBody().getMessage()));
            }
        } catch (Exception e) {
            throw new SendException(e.getMessage());
        }
        return Mono.empty();
    }

    @Override
    public void dispose() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        /** 阿里云访问密钥 AccessKey ID */
        @NotBlank
        private String accessKeyId;

        /** 阿里云访问密钥 AccessKey Secret */
        @NotBlank
        private String accessKeySecret;

        /** 短信服务 endpoint,留空使用默认值 dysmsapi.aliyuncs.com,可写 host 或 host:port */
        @Pattern(regexp = "^$|^[A-Za-z0-9.-]+(:\\d+)?$")
        private String endpoint;
    }
}
