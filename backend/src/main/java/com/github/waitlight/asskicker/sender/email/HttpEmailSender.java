package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.HashMap;
import java.util.Map;

public class HttpEmailSender extends EmailSender<HttpEmailSenderConfig> {

    private final WebClient client;

    public HttpEmailSender(HttpEmailSenderConfig config) {
        super(config);
        this.client = buildWebClient(config);
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
                    .uri(config.getPath())
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

        if (config.getFrom() != null && !config.getFrom().isBlank()) {
            body.put("from", config.getFrom());
        }

        if (request.getAttributes() != null) {
            body.putAll(request.getAttributes());
        }
        return body;
    }

    private WebClient buildWebClient(HttpEmailSenderConfig httpApi) {
        return WebClient.builder()
                .baseUrl(String.valueOf(httpApi.getBaseUrl()))
                .defaultHeader(String.valueOf(httpApi.getApiKeyHeader()), String.valueOf(httpApi.getApiKey()))
                .build();
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
