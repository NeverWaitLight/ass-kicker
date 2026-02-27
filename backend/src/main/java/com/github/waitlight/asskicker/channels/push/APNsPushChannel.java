package com.github.waitlight.asskicker.channels.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 苹果 APNs HTTP/2 推送通道。
 */
public class APNsPushChannel extends PushChannel<APNsPushChannelConfig> {

    private static final String APNS_PRODUCTION_HOST = "api.push.apple.com";
    private static final String APNS_SANDBOX_HOST = "api.sandbox.push.apple.com";
    private static final String APNS_PATH_PREFIX = "/3/device/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChannelDebugSimulator debugSimulator;

    public APNsPushChannel(APNsPushChannelConfig config, ChannelDebugSimulator debugSimulator) {
        super(config);
        this.debugSimulator = debugSimulator;
    }

    @Override
    public MsgResp send(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }
        String token = request.getRecipient();
        if (token == null || token.isBlank()) {
            return MsgResp.failure("INVALID_REQUEST", "Device token is required");
        }
        if (debugSimulator.isEnabled()) {
            return debugSimulator.simulate(getClass().getSimpleName());
        }
        try {
            PrivateKey key = loadPrivateKey();
            String jwt = buildJwt(key);
            String host = config.isProduction() ? APNS_PRODUCTION_HOST : APNS_SANDBOX_HOST;
            String url = "https://" + host + APNS_PATH_PREFIX + token;

            Map<String, Object> aps = new HashMap<>();
            Map<String, String> alert = new HashMap<>();
            if (request.getSubject() != null && !request.getSubject().isBlank()) {
                alert.put("title", request.getSubject());
            }
            alert.put("body", request.getContent() != null ? request.getContent() : "");
            aps.put("alert", alert);
            aps.put("sound", "default");
            Map<String, Object> payload = new HashMap<>();
            payload.put("aps", aps);
            byte[] bodyBytes = objectMapper.writeValueAsBytes(payload);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("authorization", "bearer " + jwt)
                    .header("apns-topic", config.getBundleId())
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .header("Content-Type", "application/json")
                    .timeout(config.getTimeout())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            int retries = config.getMaxRetries();
            Exception lastEx = null;
            for (int i = 0; i <= retries; i++) {
                try {
                    HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    int status = response.statusCode();
                    if (status == 200) {
                        String apnsId = response.headers().firstValue("apns-id").orElse(null);
                        return MsgResp.success(apnsId != null ? apnsId : "ok");
                    }
                    String reason = response.body();
                    String errorCode = categorizeApnsStatus(status, reason);
                    return MsgResp.failure(errorCode, "APNs " + status + (reason != null && !reason.isBlank() ? ": " + reason : ""));
                } catch (IOException | InterruptedException e) {
                    lastEx = e;
                    if (i < retries && isRetryable(e)) {
                        try {
                            Thread.sleep(config.getRetryDelay().toMillis());
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return MsgResp.failure("SEND_INTERRUPTED", ie.getMessage());
                        }
                    } else {
                        break;
                    }
                }
            }
            String errorCode = categorizeError(lastEx);
            return MsgResp.failure(errorCode, lastEx != null ? lastEx.getMessage() : "APNs send failed");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String content = config.getP8KeyContent();
        if (content == null || content.isBlank()) {
            String path = config.getP8KeyPath();
            if (path == null || path.isBlank()) {
                throw new IllegalStateException("APNs p8KeyContent or p8KeyPath is required");
            }
            content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        }
        content = content
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private String buildJwt(PrivateKey key) {
        long now = System.currentTimeMillis() / 1000;
        return Jwts.builder()
                .setHeaderParam("kid", config.getKeyId())
                .setIssuer(config.getTeamId())
                .setIssuedAt(new java.util.Date(now * 1000))
                .setExpiration(new java.util.Date((now + 3600) * 1000))
                .signWith(key, SignatureAlgorithm.ES256)
                .compact();
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof TimeoutException) {
            return true;
        }
        if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
            return true;
        }
        String msg = e.getMessage();
        return msg != null && (msg.contains("timeout") || msg.contains("Connection") || msg.contains("reset"));
    }

    private String categorizeApnsStatus(int status, String reason) {
        if (status == 400) {
            return "INVALID_REQUEST";
        }
        if (status == 403) {
            return "AUTHENTICATION_FAILED";
        }
        if (status == 404) {
            return "INVALID_DEVICE_TOKEN";
        }
        if (status == 410) {
            return "DEVICE_TOKEN_INACTIVE";
        }
        if (status == 429) {
            return "RATE_LIMIT_EXCEEDED";
        }
        if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "APNS_SEND_FAILED";
    }

    private String categorizeError(Exception ex) {
        if (ex instanceof TimeoutException || (ex.getCause() != null && ex.getCause() instanceof TimeoutException)) {
            return "TIMEOUT";
        }
        if (ex.getMessage() != null && ex.getMessage().contains("Connection")) {
            return "CONNECTION_FAILED";
        }
        return "APNS_SEND_FAILED";
    }
}
