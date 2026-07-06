package com.github.waitlight.asskicker.channel.impl;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ApnsPushChannel extends Channel<ApnsPushChannel.ApnsSendReq> {

    public static final ChannelType TYPE = ChannelType.APNS;
    public static final ChannelProvider PROVIDER = ChannelProvider.APPLE;

    private static final String SANDBOX_KEYWORD = "sandbox";

    private final Properties properties;
    private final ApnsClient apnsClient;

    public ApnsPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        validateSpec(this.properties);
        this.apnsClient = buildApnsClient(this.properties);
    }

    ApnsPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper, ApnsClient apnsClient) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.apnsClient = apnsClient;
    }

    /**
     * Visible for testing — bypasses Pushy ApnsClient construction so tests can inject a Mockito mock.
     */
    public static ApnsPushChannel forTesting(ChannelEntity provider, WebClient webClient,
            ObjectMapper objectMapper, ApnsClient apnsClient) {
        return new ApnsPushChannel(provider, webClient, objectMapper, apnsClient);
    }

    @Override
    public Mono<String> send(ApnsSendReq req) {
        return Mono.defer(() -> {
            List<String> recipients = req.getDeviceTokens();
            if (recipients == null || recipients.isEmpty()) {
                return Mono.error(new IllegalArgumentException("APNs deviceTokens required"));
            }

            UUID parsedApnsId = StringUtils.isBlank(properties.getApnsId())
                    ? null
                    : UUID.fromString(properties.getApnsId().trim());
            String topic = properties.getBundleIdTopic().trim();
            String payload;
            try {
                payload = buildApnsPayload(req);
            } catch (Exception e) {
                return Mono.error(e);
            }

            log.info("Sending APNs notification to {} recipient(s)", recipients.size());

            return Flux.fromIterable(recipients)
                    .concatMap(token -> {
                        SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                                token, topic, payload, null,
                                com.eatthepath.pushy.apns.DeliveryPriority.IMMEDIATE,
                                com.eatthepath.pushy.apns.PushType.ALERT, null, parsedApnsId);
                        return Mono.fromFuture(apnsClient.sendNotification(notification))
                                .map(this::extractApnsId);
                    })
                    .collect(Collectors.joining(","))
                    .map(ids -> "APNs ok " + recipients.size() + " device(s) apns-id=" + ids);
        });
    }

    @Override
    public void dispose() {
        try {
            apnsClient.close().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("APNs apnsClient close failed", e);
        }
    }

    private String extractApnsId(PushNotificationResponse<SimpleApnsPushNotification> response) {
        if (!response.isAccepted()) {
            throw new IllegalStateException("APNs " + response.getStatusCode()
                    + ": " + response.getRejectionReason().orElse(""));
        }
        UUID apnsId = response.getApnsId();
        return apnsId != null ? apnsId.toString() : "ok";
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
            throw new IllegalArgumentException("APNs recipients required");
        }
        return recipients;
    }

    private String buildApnsPayload(ApnsSendReq req) throws Exception {
        Map<String, Object> alert = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(req.getTitle())) {
            alert.put("title", req.getTitle());
        }
        alert.put("body", req.getBody() != null ? req.getBody() : "");
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", alert);
        aps.put("sound", req.getSound() != null ? req.getSound() : "default");
        if (req.getBadge() != null) {
            aps.put("badge", req.getBadge());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("aps", aps);
        Map<String, Object> customData = req.getCustomData();
        if (customData != null && !customData.isEmpty()) {
            for (Map.Entry<String, Object> e : customData.entrySet()) {
                String k = e.getKey();
                if (k != null && !k.isBlank() && !"aps".equals(k)) {
                    payload.put(k, e.getValue());
                }
            }
        }
        return objectMapper.writeValueAsString(payload);
    }

    private static void validateSpec(Properties p) {
        if (StringUtils.isBlank(p.getBundleIdTopic())
                || StringUtils.isBlank(p.getTeamId())
                || StringUtils.isBlank(p.getKeyId())
                || StringUtils.isBlank(p.getPrivateKeyPem())) {
            throw new IllegalStateException(
                    "APNs spec requires bundleIdTopic teamId keyId privateKeyPem");
        }
    }

    private static ApnsClient buildApnsClient(Properties p) {
        try {
            ApnsSigningKey signingKey = ApnsSigningKey.loadFromInputStream(
                    new ByteArrayInputStream(p.getPrivateKeyPem().getBytes(StandardCharsets.UTF_8)),
                    p.getTeamId().trim(),
                    p.getKeyId().trim());

            ApnsClientBuilder builder = new ApnsClientBuilder()
                    .setSigningKey(signingKey);

            String url = StringUtils.trimToNull(p.getUrl());
            if (url == null) {
                builder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST);
            } else if (SANDBOX_KEYWORD.equalsIgnoreCase(url)) {
                builder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST);
            } else {
                int colon = url.lastIndexOf(':');
                if (colon > 0 && colon == url.indexOf(':') && !url.contains("://")) {
                    builder.setApnsServer(url.substring(0, colon), Integer.parseInt(url.substring(colon + 1)));
                } else {
                    String host = url.replaceFirst("^https?://", "").replaceAll("/.*$", "");
                    int hostColon = host.indexOf(':');
                    if (hostColon > 0) {
                        builder.setApnsServer(host.substring(0, hostColon),
                                Integer.parseInt(host.substring(hostColon + 1)));
                    } else {
                        builder.setApnsServer(host);
                    }
                }
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("APNs ApnsClient init failed: " + e.getMessage(), e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        private String url;

        @NotBlank
        private String bundleIdTopic;

        @NotBlank
        private String teamId;

        @NotBlank
        private String keyId;

        @NotBlank
        private String privateKeyPem;

        @Pattern(regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private String apnsId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class ApnsSendReq extends SendReq {
        private List<String> deviceTokens;
        private String title;
        private String body;
        private Integer badge;
        private String sound;
        private Map<String, Object> customData;
    }
}
