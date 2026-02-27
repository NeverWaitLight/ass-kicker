package com.github.waitlight.asskicker.channels.email;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class HttpEmailChannel extends EmailChannel<HttpEmailChannelConfig> {

    private final WebClient client;
    private final ChannelDebugSimulator debugSimulator;

    public HttpEmailChannel(HttpEmailChannelConfig config, WebClient webClient, ChannelDebugSimulator debugSimulator) {
        super(config);
        this.client = webClient;
        this.debugSimulator = debugSimulator;
    }

    @Override
    public MsgResp send(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }
        if (debugSimulator.isEnabled()) {
            return debugSimulator.simulate(getClass().getSimpleName());
        }
        try {
            Map<String, Object> body = buildRequestBody(request);
            String fullUri = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl())
                    .path(config.getPath().startsWith("/") ? config.getPath() : "/" + config.getPath())
                    .build()
                    .toUriString();

            String messageId = client
                    .post()
                    .uri(fullUri)
                    .header(config.getApiKeyHeader(), config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(config.getTimeout())
                    .retryWhen(Retry.fixedDelay(config.getMaxRetries(), config.getRetryDelay())
                            .filter(this::isRetryableException))
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.error(new RuntimeException(
                                    String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()),
                                    ex)))
                    .onErrorResume(ex -> Mono.error(new RuntimeException(ex.getMessage(), ex)))
                    .block();
            return MsgResp.success(messageId);
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MsgResp.failure(errorCode, ex.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(MsgReq request) {
        Map<String, Object> body = new HashMap<>();
        body.put("to", String.valueOf(request.getRecipient()));
        body.put("subject", String.valueOf(request.getSubject()));
        body.put("content", String.valueOf(request.getContent()));

        if (config.getFrom() != null && !config.getFrom().isBlank()) {
            body.put("from", config.getFrom());
        }

        if (request.getAttributes() != null) {
            body.putAll(request.getAttributes());
        }
        return body;
    }

    private boolean isRetryableException(Throwable ex) {
        if (ex instanceof WebClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            // Only retry on server errors (5xx) and rate limiting (429), NOT on auth errors (401/403)
            return status == 429 || status == 503 || (status >= 500 && status < 600);
        }
        return ex instanceof java.net.ConnectException
                || ex instanceof java.util.concurrent.TimeoutException;
    }

    private String categorizeError(Exception ex) {
        WebClientResponseException responseEx = findCause(ex, WebClientResponseException.class);
        if (responseEx != null) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        }
        if (findCause(ex, java.net.ConnectException.class) != null) {
            return "CONNECTION_FAILED";
        }
        if (findCause(ex, java.util.concurrent.TimeoutException.class) != null) {
            return "TIMEOUT";
        }
        return "MAIL_SEND_FAILED";
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

    private String categorizeHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return "AUTHENTICATION_FAILED";
        } else if (status == 400) {
            return "INVALID_REQUEST";
        } else if (status == 429) {
            return "RATE_LIMIT_EXCEEDED";
        } else if (status >= 500) {
            return "SERVER_ERROR";
        }
        return "MAIL_SEND_FAILED";
    }
}
