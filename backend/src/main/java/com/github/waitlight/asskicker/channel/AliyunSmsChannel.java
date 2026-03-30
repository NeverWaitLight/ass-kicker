package com.github.waitlight.asskicker.channel;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.aliyun.AliyunRpcSigner;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AliyunSmsChannel extends Channel {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final Spec spec;

    public AliyunSmsChannel(ChannelProviderEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = AliyunSmsSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress, "ALIYUN_SMS");
            validateSpec();

            String templateCode = resolveTemplateCode(uniMessage);
            if (StringUtils.isBlank(templateCode)) {
                return Mono.error(new IllegalStateException("ALIYUN_SMS requires templateCode in channel properties or extraData.templateCode"));
            }

            String templateParamJson;
            try {
                templateParamJson = buildTemplateParamJson(uniMessage);
            } catch (Exception e) {
                return Mono.error(e);
            }

            String baseUrl = StringUtils.isNotBlank(spec.endpoint()) ? spec.endpoint().trim() : "https://dysmsapi.aliyuncs.com";

            return Flux.fromIterable(recipients)
                    .concatMap(phone -> sendOne(baseUrl, phone, templateCode, templateParamJson))
                    .collect(Collectors.joining(","))
                    .map(ignore -> "ALIYUN_SMS ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(spec.accessKeyId()) || StringUtils.isBlank(spec.accessKeySecret())
                || StringUtils.isBlank(spec.signName())) {
            throw new IllegalStateException("ALIYUN_SMS requires accessKeyId accessKeySecret signName");
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

    private Mono<String> sendOne(String baseUrl, String phoneNumbers, String templateCode, String templateParamJson) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("AccessKeyId", spec.accessKeyId().trim());
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("PhoneNumbers", phoneNumbers);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("SignName", spec.signName().trim());
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", templateParamJson);
        params.put("Timestamp", TIMESTAMP_FMT.format(Instant.now()));
        params.put("Version", "2017-05-25");
        if (StringUtils.isNotBlank(spec.regionId())) {
            params.put("RegionId", spec.regionId().trim());
        }

        AliyunRpcSigner.sign(params, "POST", spec.accessKeySecret().trim());
        String body = AliyunRpcSigner.toFormBody(params);

        return webClient.post()
                .uri(baseUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body.getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(this::parseAliyunSmsResponse)
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        "ALIYUN_SMS " + ex.getStatusCode().value()
                                + (StringUtils.isNotBlank(ex.getResponseBodyAsString())
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    private Mono<String> parseAliyunSmsResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String code = textOrNull(root.get("Code"));
            if ("OK".equalsIgnoreCase(code)) {
                return Mono.just("ok");
            }
            String msg = textOrNull(root.get("Message"));
            return Mono.error(new IllegalStateException("ALIYUN_SMS failure Code=" + code + " Message=" + msg));
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("ALIYUN_SMS invalid response: " + responseBody, e));
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
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

    record Spec(String accessKeyId, String accessKeySecret, String signName, String templateCode, String regionId,
            String endpoint) {
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
