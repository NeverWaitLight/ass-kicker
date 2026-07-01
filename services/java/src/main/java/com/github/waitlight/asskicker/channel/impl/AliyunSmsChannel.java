package com.github.waitlight.asskicker.channel.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AliyunSmsChannel extends Channel {

    private static final String DEFAULT_ENDPOINT = "dysmsapi.aliyuncs.com";

    private final Properties properties;
    private final Client aliyunClient;

    public AliyunSmsChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        try {
            this.aliyunClient = new Client(new Config()
                    .setAccessKeyId(properties.getAccessKeyId())
                    .setAccessKeySecret(properties.getAccessKeySecret())
                    .setEndpoint(StringUtils.defaultIfBlank(properties.getEndpoint(), DEFAULT_ENDPOINT)));
        } catch (Exception e) {
            throw new IllegalStateException("ALIYUN_SMS SDK client init failed", e);
        }
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress);
            String templateCode = resolveTemplateCode(uniMessage);
            if (StringUtils.isBlank(templateCode)) {
                return Mono.error(new IllegalStateException(
                        "ALIYUN_SMS requires templateCode in properties or extraData"));
            }
            String templateParamJson = buildTemplateParamJson(uniMessage);
            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(phone, templateCode, templateParamJson))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "ALIYUN_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private String resolveTemplateCode(UniMessage uniMessage) {
        if (uniMessage != null && uniMessage.getExtraData() != null) {
            Object o = uniMessage.getExtraData().get("templateCode");
            if (o != null && StringUtils.isNotBlank(String.valueOf(o))) {
                return String.valueOf(o);
            }
        }
        return properties.getTemplateCode();
    }

    private String buildTemplateParamJson(UniMessage uniMessage) {
        Map<String, Object> params = uniMessage != null ? uniMessage.getTemplateParams() : null;
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            throw new IllegalStateException("ALIYUN_SMS templateParam serialize failed", e);
        }
    }

    private Mono<String> sendOne(String phoneNumbers, String templateCode, String templateParamJson) {
        return Mono.fromCallable(() -> {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumbers)
                    .setSignName(properties.getSignName())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParamJson);
            SendSmsResponse response = aliyunClient.sendSms(request);
            SendSmsResponseBody body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("ALIYUN_SMS empty response body");
            }
            if ("OK".equalsIgnoreCase(body.getCode())) {
                return "ok";
            }
            throw new IllegalStateException(
                    "ALIYUN_SMS failure Code=" + body.getCode() + " Message=" + body.getMessage());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<String> normalizeRecipients(UniAddress uniAddress) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("ALIYUN_SMS recipients required");
        }
        return recipients;
    }

    @Schema(description = "阿里云短信通道配置")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        @Schema(description = "AccessKey ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String accessKeyId;

        @Schema(description = "AccessKey Secret", requiredMode = Schema.RequiredMode.REQUIRED, type = "password")
        @NotBlank
        private String accessKeySecret;

        @Schema(description = "短信签名", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String signName;

        @Schema(description = "短信模板 Code")
        private String templateCode;

        @Schema(description = "服务接入点")
        @Pattern(regexp = "^$|^[A-Za-z0-9.-]+(:\\d+)?$")
        private String endpoint;
    }
}
