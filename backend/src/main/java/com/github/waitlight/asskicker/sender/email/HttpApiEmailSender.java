package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.Sender;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.HashMap;
import java.util.Map;

public class HttpApiEmailSender implements Sender {

    private final WebClient client;

    private final EmailSenderProperties.HttpApi properties;

    public HttpApiEmailSender(WebClient.Builder builder, EmailSenderProperties.HttpApi properties) {
        this.properties = properties;
        this.client = builder
                .baseUrl(String.valueOf(properties.getBaseUrl()))
                .defaultHeader(String.valueOf(properties.getApiKeyHeader()), String.valueOf(properties.getApiKey()))
                .build();
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (request == null) {
            return MessageResponse.failure("INVALID_REQUEST", "Message request is null");
        }
        try {
            Map<String, Object> body = buildRequestBody(request);

            String messageId = client
                    .post()
                    .uri(properties.getPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(properties.getTimeout())
                    .retryWhen(Retry.fixedDelay(properties.getMaxRetries(), properties.getRetryDelay())
                            .filter(this::isRetryableException))
                    .onErrorResume(WebClientResponseException.class, ex ->
                            Mono.error(new RuntimeException(
                                    String.format("HTTP %d: %s", ex.getStatusCode().value(), ex.getResponseBodyAsString()),
                                    ex)))
                    .onErrorResume(ex -> Mono.error(new RuntimeException(ex.getMessage(), ex)))
                    .block();
            return MessageResponse.success(messageId);
        } catch (Exception ex) {
            String errorCode = categorizeError(ex);
            return MessageResponse.failure(errorCode, ex.getMessage());
        }
    }

    private Map<String, Object> buildRequestBody(MessageRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("to", String.valueOf(request.getRecipient()));
        body.put("subject", String.valueOf(request.getSubject()));
        body.put("content", String.valueOf(request.getContent()));

        if (properties.getFrom() != null && !properties.getFrom().isBlank()) {
            body.put("from", properties.getFrom());
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
        Throwable cause = ex.getCause();

        if (ex instanceof WebClientResponseException responseEx) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        } else if (cause instanceof WebClientResponseException responseEx) {
            return categorizeHttpStatus(responseEx.getStatusCode().value());
        } else if (cause instanceof java.net.ConnectException) {
            return "CONNECTION_FAILED";
        } else if (cause instanceof java.util.concurrent.TimeoutException) {
            return "TIMEOUT";
        }
        return "MAIL_SEND_FAILED";
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

