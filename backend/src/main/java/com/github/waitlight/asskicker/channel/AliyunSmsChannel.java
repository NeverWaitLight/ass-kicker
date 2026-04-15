package com.github.waitlight.asskicker.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ChannelImpl(providerType = ProviderType.ALIYUN_SMS, propertyClass = AliyunSmsChannel.Spec.class)
public class AliyunSmsChannel extends Channel {

    private final Spec spec;
    private final Client aliyunClient;

    public AliyunSmsChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = AliyunSmsSpecMapper.INSTANCE.toSpec(provider.getProperties());
        try {
            this.aliyunClient = new Client(buildConfig(spec));
        } catch (Exception e) {
            throw new IllegalStateException("ALIYUN_SMS SDK client init failed", e);
        }
    }

    private static Config buildConfig(Spec spec) throws Exception {
        Config config = new Config();
        config.setAccessKeyId(spec.accessKeyId().trim());
        config.setAccessKeySecret(spec.accessKeySecret().trim());
        applyEndpoint(config, spec.endpoint().trim());
        if (StringUtils.isNotBlank(spec.regionId())) {
            config.setRegionId(spec.regionId().trim());
        }
        return config;
    }

    /**
     * Tea 客户端使用 host 或 host:port；支持完整 URL（含 http/https）或裸域名。
     */
    static void applyEndpoint(Config config, String endpointRaw) {
        String s = endpointRaw.trim();
        URI uri = URI.create(s.contains("://") ? s : "https://" + s);
        String scheme = uri.getScheme();
        if (scheme != null) {
            config.setProtocol(scheme.toLowerCase());
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new IllegalStateException("ALIYUN_SMS endpoint has no host: " + endpointRaw);
        }
        int port = uri.getPort();
        if (port > 0) {
            boolean defaultHttps = "https".equalsIgnoreCase(scheme) && port == 443;
            boolean defaultHttp = "http".equalsIgnoreCase(scheme) && port == 80;
            if (!defaultHttps && !defaultHttp) {
                config.setEndpoint(host + ":" + port);
                return;
            }
        }
        config.setEndpoint(host);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress, "ALIYUN_SMS");
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

            String tpl = templateCode;
            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(phone, tpl, templateParamJson))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "ALIYUN_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(spec.accessKeyId()) || StringUtils.isBlank(spec.accessKeySecret())
                || StringUtils.isBlank(spec.signName())) {
            throw new IllegalStateException("ALIYUN_SMS requires accessKeyId accessKeySecret signName");
        }
        if (StringUtils.isBlank(spec.endpoint())) {
            throw new IllegalStateException("ALIYUN_SMS requires endpoint");
        }
    }

    private String resolveTemplateCode(UniMessage uniMessage) {
        if (uniMessage != null && uniMessage.getExtraData() != null) {
            Object o = uniMessage.getExtraData().get("templateCode");
            if (o != null && StringUtils.isNotBlank(String.valueOf(o))) {
                return String.valueOf(o).trim();
            }
        }
        return spec.templateCode();
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
                    .setSignName(spec.signName().trim())
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

    private List<String> normalizeRecipients(UniAddress uniAddress, String providerName) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(providerName + " recipients required");
        }
        return recipients;
    }

    record Spec(
            @NotBlank(message = "accessKeyId 不能为空") String accessKeyId,
            @NotBlank(message = "accessKeySecret 不能为空") String accessKeySecret,
            @NotBlank(message = "signName 不能为空") String signName,
            String templateCode,
            String regionId,
            @NotBlank(message = "endpoint 不能为空") String endpoint) {
    }
}

@Mapper
interface AliyunSmsSpecMapper {
    AliyunSmsSpecMapper INSTANCE = Mappers.getMapper(AliyunSmsSpecMapper.class);

    @Mapping(target = "accessKeyId", source = "properties.accessKeyId")
    @Mapping(target = "accessKeySecret", source = "properties.accessKeySecret")
    @Mapping(target = "signName", source = "properties.signName")
    @Mapping(target = "templateCode", source = "properties.templateCode")
    @Mapping(target = "regionId", source = "properties.regionId")
    @Mapping(target = "endpoint", source = "properties.endpoint")
    AliyunSmsChannel.Spec toSpec(Map<String, String> properties);
}
