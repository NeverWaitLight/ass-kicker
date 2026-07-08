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
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.service.RecordService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Channel(type = ChannelType.APNS, provider = ChannelProvider.APPLE, reqType = PushReq.class)
public class ApnsPushChannel extends AbstractChannel<PushReq> {

    private static final String SANDBOX_KEYWORD = "sandbox";

    private final Properties properties;
    private final ApnsClient apnsClient;

    public ApnsPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
                           RecordService recordService) {
        super(provider, webClient, objectMapper, recordService);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        validateSpec(this.properties);
        this.apnsClient = buildApnsClient(this.properties);
    }

    ApnsPushChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
                    RecordService recordService, ApnsClient apnsClient) {
        super(provider, webClient, objectMapper, recordService);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.apnsClient = apnsClient;
    }

    public static ApnsPushChannel forTesting(ChannelEntity provider, WebClient webClient,
            ObjectMapper objectMapper, RecordService recordService, ApnsClient apnsClient) {
        return new ApnsPushChannel(provider, webClient, objectMapper, recordService, apnsClient);
    }

    @Override
    protected Mono<String> doSend(PushReq req) {
        return Mono.defer(() -> {
            String token = StringUtils.trimToNull(req.getDeviceToken());
            if (token == null) {
                return Mono.error(new IllegalArgumentException("APNs deviceToken required"));
            }

            UUID apnsId = StringUtils.isBlank(properties.getApnsId())
                    ? null
                    : UUID.fromString(properties.getApnsId().trim());
            String topic = properties.getBundleIdTopic().trim();
            DeliveryPriority priority = resolveDeliveryPriority(req.getPriority());
            String payload = buildApnsPayload(req);

            log.info("Sending APNs notification to device token ***{}",
                    token.length() > 6 ? token.substring(token.length() - 6) : token);

            SimpleApnsPushNotification notification = new SimpleApnsPushNotification(
                    token, topic, payload, null, priority, PushType.ALERT, null, apnsId);
            return Mono.fromFuture(apnsClient.sendNotification(notification))
                    .map(this::extractApnsId)
                    .map(id -> "APNs ok apns-id=" + id);
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

    private String buildApnsPayload(PushReq req) {
        SimpleApnsPayloadBuilder builder = new SimpleApnsPayloadBuilder();
        if (StringUtils.isNotBlank(req.getTitle())) {
            builder.setAlertTitle(req.getTitle());
        }
        builder.setAlertBody(req.getBody() != null ? req.getBody() : "");
        builder.setSound("default");

        Map<String, Object> data = req.getData();
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, Object> e : data.entrySet()) {
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
    public static class Properties {

        /** APNs 服务器地址:留空走生产环境;"sandbox" 走开发环境;也可显式写 host 或 host:port */
        private String url;

        /** iOS App 的 bundle id,作为 APNs topic 使用 */
        @NotBlank
        private String bundleIdTopic;

        /** Apple 开发者账号的 team id(10 位字符) */
        @NotBlank
        private String teamId;

        /** APNs 认证私钥的 key id(10 位字符),对应 Apple Developer 控制台生成的 Key */
        @NotBlank
        private String keyId;

        /** APNs 认证私钥的 PEM 内容(.p8 文件全文,含 BEGIN/END PRIVATE KEY 行) */
        @NotBlank
        private String privateKeyPem;

        /** 账号级默认 apns-id(UUID 格式),用于服务端去重;留空则每次请求由 APNs 生成 */
        @Pattern(regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private String apnsId;
    }
}
