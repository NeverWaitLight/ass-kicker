package com.github.waitlight.asskicker.channel.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelImpl;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ChannelImpl(providerType = ProviderType.ALIYUN_SMS, propertyClass = AliyunSmsChannel.Properties.class)
public class AliyunSmsChannel extends Channel {

    private final Properties properties;
    private final Client aliyunClient;

    public AliyunSmsChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        try {
            this.aliyunClient = new Client(buildConfig(properties));
        } catch (Exception e) {
            throw new IllegalStateException("ALIYUN_SMS SDK client init failed", e);
        }
    }

    private static Config buildConfig(Properties properties) {
        Config config = new Config()
                .setAccessKeyId(properties.accessKeyId().trim())
                .setAccessKeySecret(properties.accessKeySecret().trim());
        if (StringUtils.isNotBlank(properties.regionId())) {
            config.setRegionId(properties.regionId().trim());
        }
        if (StringUtils.isNotBlank(properties.endpoint())) {
            applyEndpoint(config, properties.endpoint().trim());
        }
        return config;
    }

    /**
     * Endpoint 接受裸 host[:port] 或带 scheme 的 URL；带 scheme 时 protocol 也会被同步覆盖。
     */
    static void applyEndpoint(Config config, String endpointRaw) {
        if (!endpointRaw.contains("://")) {
            config.setEndpoint(endpointRaw);
            return;
        }
        URI uri = URI.create(endpointRaw);
        if (uri.getScheme() != null) {
            config.setProtocol(uri.getScheme().toLowerCase());
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalStateException("ALIYUN_SMS endpoint has no host: " + endpointRaw);
        }
        int port = uri.getPort();
        config.setEndpoint(port > 0 ? host + ":" + port : host);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress);
            validateSpec();

            String templateCode = resolveTemplateCode(uniMessage);
            if (StringUtils.isBlank(templateCode)) {
                return Mono.error(new IllegalStateException(
                        "ALIYUN_SMS requires templateCode in channel properties or extraData.templateCode"));
            }

            String templateParamJson;
            try {
                templateParamJson = buildTemplateParamJson(uniMessage);
            } catch (Exception e) {
                return Mono.error(e);
            }

            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(phone, templateCode, templateParamJson))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "ALIYUN_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(properties.accessKeyId()) || StringUtils.isBlank(properties.accessKeySecret())
                || StringUtils.isBlank(properties.signName())) {
            throw new IllegalStateException("ALIYUN_SMS requires accessKeyId accessKeySecret signName");
        }
        if (StringUtils.isBlank(properties.regionId()) && StringUtils.isBlank(properties.endpoint())) {
            throw new IllegalStateException("ALIYUN_SMS requires regionId or endpoint");
        }
    }

    private String resolveTemplateCode(UniMessage uniMessage) {
        if (uniMessage != null && uniMessage.getExtraData() != null) {
            Object o = uniMessage.getExtraData().get("templateCode");
            if (o != null && StringUtils.isNotBlank(String.valueOf(o))) {
                return String.valueOf(o).trim();
            }
        }
        return properties.templateCode();
    }

    private String buildTemplateParamJson(UniMessage uniMessage) throws Exception {
        Map<String, Object> params = uniMessage != null ? uniMessage.getTemplateParams() : null;
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        return objectMapper.writeValueAsString(params);
    }

    private Mono<String> sendOne(String phoneNumbers, String templateCode, String templateParamJson) {
        return Mono.fromCallable(() -> {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumbers)
                    .setSignName(properties.signName().trim())
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParamJson);
            SendSmsResponse response = aliyunClient.sendSms(request);
            SendSmsResponseBody body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("ALIYUN_SMS empty response body");
            }
            String code = body.getCode();
            if ("OK".equalsIgnoreCase(code)) {
                return "ok";
            }
            throw new IllegalStateException(
                    "ALIYUN_SMS failure Code=" + code + " Message=" + body.getMessage());
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

    record Properties(
            @NotBlank String accessKeyId,
            @NotBlank String accessKeySecret,
            @NotBlank String signName,
            String templateCode,
            String regionId,
            String endpoint) {
    }
}
