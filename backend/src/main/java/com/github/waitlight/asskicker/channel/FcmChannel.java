package com.github.waitlight.asskicker.channel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class FcmChannel extends Channel {

    private final Spec spec;

    public FcmChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = FcmSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                    ? List.of()
                    : uniAddress.getRecipients().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.toList());

            if (recipients.isEmpty()) {
                return Mono.error(new IllegalArgumentException("FCM recipients required"));
            }

            if (StringUtils.isBlank(spec.url())
                    || StringUtils.isBlank(spec.projectId())
                    || StringUtils.isBlank(spec.accessToken())) {
                return Mono.error(new IllegalStateException(
                        "FCM spec requires url projectId accessToken"));
            }

            String alertTitle = uniMessage != null ? uniMessage.getTitle() : null;
            String alertBody = uniMessage != null ? uniMessage.getContent() : null;
            Map<String, Object> extraData = uniMessage != null ? uniMessage.getExtraData() : null;

            log.info("Sending FCM notification to {} recipient(s)", recipients.size());

            String endpoint = buildFcmEndpoint(spec.url().trim(), spec.projectId().trim());

            return Flux.fromIterable(recipients)
                    .concatMap(token -> {
                        byte[] bodyBytes;
                        try {
                            bodyBytes = buildFcmPayloadBytes(objectMapper, token, alertTitle, alertBody, extraData);
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                        return postFcmMessage(spec.accessToken().trim(), endpoint, bodyBytes);
                    })
                    .collect(Collectors.joining(","))
                    .map(names -> "FCM ok " + recipients.size() + " device(s) name=" + names);
        });
    }

    private static String buildFcmEndpoint(String url, String projectId) {
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return base + "/v1/projects/" + projectId + "/messages:send";
    }

    private static byte[] buildFcmPayloadBytes(ObjectMapper objectMapper, String token, String title, String body,
            Map<String, Object> extraData) throws Exception {
        Map<String, Object> notification = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(title)) {
            notification.put("title", title);
        }
        notification.put("body", body != null ? body : "");

        Map<String, Object> androidNotification = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(title)) {
            androidNotification.put("title", title);
        }
        androidNotification.put("body", body != null ? body : "");

        Map<String, Object> android = new LinkedHashMap<>();
        android.put("priority", "HIGH");
        android.put("notification", androidNotification);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", token);
        message.put("notification", notification);
        message.put("android", android);

        if (extraData != null && !extraData.isEmpty()) {
            Map<String, String> dataMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : extraData.entrySet()) {
                String k = e.getKey();
                if (k != null && !k.isBlank()) {
                    dataMap.put(k, e.getValue() != null ? e.getValue().toString() : "");
                }
            }
            if (!dataMap.isEmpty()) {
                message.put("data", dataMap);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        return objectMapper.writeValueAsBytes(payload);
    }

    private Mono<String> postFcmMessage(String accessToken, String endpoint, byte[] bodyBytes) {
        return webClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyBytes)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);
                        Object name = resp.get("name");
                        return name != null ? name.toString() : "ok";
                    } catch (Exception e) {
                        return "ok";
                    }
                })
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        "FCM " + ex.getStatusCode().value()
                                + (ex.getResponseBodyAsString() != null && !ex.getResponseBodyAsString().isBlank()
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    record Spec(
            String url,
            String projectId,
            String accessToken) {
    }
}

@Mapper
interface FcmSpecMapper {
    FcmSpecMapper INSTANCE = Mappers.getMapper(FcmSpecMapper.class);

    @Mapping(target = "url", source = "properties.url")
    @Mapping(target = "projectId", source = "properties.projectId")
    @Mapping(target = "accessToken", source = "properties.accessToken")
    FcmChannel.Spec toSpec(Map<String, String> properties);
}
