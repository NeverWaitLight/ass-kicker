package com.github.waitlight.asskicker.channel.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class FcmPushChannel extends Channel<SendReq> {

    public static final ChannelType TYPE = ChannelType.FCM;
    public static final ChannelProvider PROVIDER = ChannelProvider.GOOGLE;

    private static final String FIREBASE_MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String FCM_ENDPOINT_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final Properties properties;
    private final GoogleCredentials credentials;
    private final String endpoint;

    public FcmPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        validateSpec(this.properties);
        this.credentials = loadCredentials(this.properties.getServiceAccountJson());
        this.endpoint = String.format(FCM_ENDPOINT_TEMPLATE, this.properties.getProjectId().trim());
    }

    // Package-private constructor for tests: lets MockWebServer intercept and inject fake credentials
    FcmPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
            GoogleCredentials credentials, String endpoint) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.credentials = credentials;
        this.endpoint = endpoint;
    }

    /**
     * Visible for testing — bypasses serviceAccountJson parsing and lets callers point the channel
     * at a custom endpoint (e.g. MockWebServer) with pre-baked GoogleCredentials.
     */
    public static FcmPushChannel forTesting(ChannelEntity provider, WebClient webClient,
            ObjectMapper objectMapper, GoogleCredentials credentials, String endpoint) {
        return new FcmPushChannel(provider, webClient, objectMapper, credentials, endpoint);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress);

            String alertTitle = uniMessage != null ? uniMessage.getTitle() : null;
            String alertBody = uniMessage != null ? uniMessage.getContent() : null;
            Map<String, Object> extraData = uniMessage != null ? uniMessage.getExtraData() : null;

            log.info("Sending FCM notification to {} recipient(s)", recipients.size());

            return Mono.fromCallable(this::resolveAccessToken)
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(token -> Flux.fromIterable(recipients)
                            .concatMap(deviceToken -> {
                                byte[] bodyBytes;
                                try {
                                    bodyBytes = buildFcmPayloadBytes(objectMapper, deviceToken, alertTitle, alertBody,
                                            extraData);
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }
                                return postFcmMessage(token, endpoint, bodyBytes);
                            }))
                    .collect(Collectors.joining(","))
                    .map(names -> "FCM ok " + recipients.size() + " device(s) name=" + names);
        });
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
            throw new IllegalArgumentException("FCM recipients required");
        }
        return recipients;
    }

    private static void validateSpec(Properties p) {
        if (StringUtils.isBlank(p.getProjectId()) || StringUtils.isBlank(p.getServiceAccountJson())) {
            throw new IllegalStateException("FCM spec requires projectId serviceAccountJson");
        }
    }

    private static GoogleCredentials loadCredentials(String serviceAccountJson) {
        try {
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(FIREBASE_MESSAGING_SCOPE);
        } catch (IOException e) {
            throw new IllegalStateException("FCM credentials init failed", e);
        }
    }

    private String resolveAccessToken() throws IOException {
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        @NotBlank
        private String projectId;

        @NotBlank
        private String serviceAccountJson;
    }
}
