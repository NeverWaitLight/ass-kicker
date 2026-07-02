package com.github.waitlight.asskicker.channel.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
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

public class AliyunSmsChannel extends Channel<SmsSendReq> {

    public static final ChannelType TYPE = ChannelType.SMS;
    public static final ChannelProvider PROVIDER = ChannelProvider.ALIYUN;

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
    public Mono<String> send(SmsSendReq req) {
        try {
            String templateParam = req.getTemplateParam() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(req.getTemplateParam());

            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(req.getPhoneNumber())
                    .setSignName(req.getSignName())
                    .setTemplateCode(req.getTemplateCode())
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
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.empty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        @NotBlank
        private String accessKeyId;

        @NotBlank
        private String accessKeySecret;

        @Pattern(regexp = "^$|^[A-Za-z0-9.-]+(:\\d+)?$")
        private String endpoint;
    }
}
