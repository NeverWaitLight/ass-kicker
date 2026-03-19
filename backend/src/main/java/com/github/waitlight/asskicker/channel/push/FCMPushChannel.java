package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.MsgReq;
import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.ByteArrayInputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 谷歌 FCM HTTP v1 推送通道。
 */
public class FCMPushChannel extends Channel<FCMPushChannelSpec> {

    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String FCM_SEND_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final WebClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FCMPushChannel(FCMPushChannelSpec spec, WebClient webClient, ChannelDebugProperties debugProperties) {
        super(spec, debugProperties);
        this.client = webClient;
    }

    @Override
    protected MsgResp doSend(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }
        String token = request.getRecipient();
        if (token == null || token.isBlank()) {
            return MsgResp.failure("INVALID_REQUEST", "FCM token is required");
        }
        try {
            String projectId = resolveProjectId();
            String accessToken = getAccessToken();
            String url = String.format(FCM_SEND_URL, projectId);

            Map<String, Object> notification = new HashMap<>();
            if (request.getSubject() != null && !request.getSubject().isBlank()) {
                notification.put("title", request.getSubject());
            }
            notification.put("body", request.getContent() != null ? request.getContent() : "");

            Map<String, Object> message = new HashMap<>();
            message.put("token", token);
            message.put("notification", notification);

            Map<String, Object> body = new HashMap<>();
            body.put("message", message);

            String responseStr = client.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(spec.getTimeout())
                    .retryWhen(Retry.fixedDelay(spec.getMaxRetries(), spec.getRetryDelay())
                            .filter(this::isRetryableException))
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.error(new RuntimeException(
                                    String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()),
                                    ex)))
                    .block();

            if (responseStr != null && !responseStr.isBlank()) {
                JsonNode node = objectMapper.readTree(responseStr);
                JsonNode name = node.get("name");
                if (name != null && name.isTextual()) {
                    return MsgResp.success(name.asText());
                }
            }
            return MsgResp.success("ok");
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private String resolveProjectId() throws Exception {
        if (spec.getProjectId() != null && !spec.getProjectId().isBlank()) {
            return spec.getProjectId();
        }
        JsonNode root = objectMapper.readTree(spec.getServiceAccountJson());
        JsonNode projectId = root.get("project_id");
        if (projectId != null && projectId.isTextual()) {
            return projectId.asText();
        }
        throw new IllegalStateException("FCM projectId not set and not found in serviceAccountJson");
    }

    private String getAccessToken() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(spec.getServiceAccountJson().getBytes(StandardCharsets.UTF_8)))
                .createScoped(List.of(FCM_SCOPE));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof WebClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            return status == 429 || status == 503 || (status >= 500 && status < 600);
        }
        return ex instanceof ConnectException
                || ex instanceof TimeoutException;
    }

    private String categorizeError(Exception ex) {
        WebClientResponseException responseEx = findCause(ex, WebClientResponseException.class);
        if (responseEx != null) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        }
        if (findCause(ex, ConnectException.class) != null) {
            return "CONNECTION_FAILED";
        }
        if (findCause(ex, TimeoutException.class) != null) {
            return "TIMEOUT";
        }
        return "FCM_SEND_FAILED";
    }

    private String categorizeHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return "AUTHENTICATION_FAILED";
        }
        if (status == 400) {
            return "INVALID_REQUEST";
        }
        if (status == 404) {
            return "INVALID_DEVICE_TOKEN";
        }
        if (status == 429) {
            return "RATE_LIMIT_EXCEEDED";
        }
        if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "FCM_SEND_FAILED";
    }

    private <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
