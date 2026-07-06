package com.github.waitlight.asskicker.channel.impl;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.SendReq;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

            UUID defaultApnsId = StringUtils.isBlank(properties.getApnsId())
                    ? null
                    : UUID.fromString(properties.getApnsId().trim());
            UUID reqApnsId = StringUtils.isBlank(req.getApnsId())
                    ? null
                    : UUID.fromString(req.getApnsId().trim());
            UUID apnsId = reqApnsId != null ? reqApnsId : defaultApnsId;

            String topic = properties.getBundleIdTopic().trim();
            DeliveryPriority priority = resolveDeliveryPriority(req.getPriority());
            PushType pushType = resolvePushType(req.getPushType());
            Instant invalidationTime = req.getExpirationEpochMillis() == null
                    ? null
                    : Instant.ofEpochMilli(req.getExpirationEpochMillis());
            String collapseId = StringUtils.trimToNull(req.getCollapseId());

            String payload = buildApnsPayload(req);

            log.info("Sending APNs notification to {} recipient(s)", recipients.size());

            return Flux.fromIterable(recipients)
                    .concatMap(token -> {
                        SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                                token, topic, payload, invalidationTime,
                                priority, pushType, collapseId, apnsId);
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

    private String buildApnsPayload(ApnsSendReq req) {
        SimpleApnsPayloadBuilder builder = new SimpleApnsPayloadBuilder();

        if (StringUtils.isNotBlank(req.getTitle())) {
            builder.setAlertTitle(req.getTitle());
        }
        if (StringUtils.isNotBlank(req.getSubtitle())) {
            builder.setAlertSubtitle(req.getSubtitle());
        }
        builder.setAlertBody(req.getBody() != null ? req.getBody() : "");
        builder.setSound(req.getSound() != null ? req.getSound() : "default");
        if (req.getBadge() != null) {
            builder.setBadgeNumber(req.getBadge());
        }
        if (StringUtils.isNotBlank(req.getCategory())) {
            builder.setCategoryName(req.getCategory());
        }
        if (StringUtils.isNotBlank(req.getThreadId())) {
            builder.setThreadId(req.getThreadId());
        }
        if (req.getContentAvailable() != null) {
            builder.setContentAvailable(req.getContentAvailable());
        }
        if (req.getMutableContent() != null) {
            builder.setMutableContent(req.getMutableContent());
        }

        Map<String, Object> customData = req.getCustomData();
        if (customData != null && !customData.isEmpty()) {
            for (Map.Entry<String, Object> e : customData.entrySet()) {
                String k = e.getKey();
                if (k != null && !k.isBlank() && !"aps".equals(k)) {
                    builder.addCustomProperty(k, e.getValue());
                }
            }
        }
        return builder.build();
    }

    private static DeliveryPriority resolveDeliveryPriority(String value) {
        if (StringUtils.isBlank(value)) {
            return DeliveryPriority.IMMEDIATE;
        }
        try {
            return DeliveryPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "APNs unsupported priority: " + value + " (allowed: IMMEDIATE, CONSERVE_POWER)", ex);
        }
    }

    private static PushType resolvePushType(String value) {
        if (StringUtils.isBlank(value)) {
            return PushType.ALERT;
        }
        try {
            return PushType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("APNs unsupported pushType: " + value, ex);
        }
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
        private String subtitle;
        private String body;
        private Integer badge;
        private String sound;
        private String category;
        private String threadId;
        private String collapseId;
        private Boolean contentAvailable;
        private Boolean mutableContent;
        private String priority;
        private String pushType;
        private String apnsId;
        private Long expirationEpochMillis;
        private Map<String, Object> customData;
    }
}
