package com.github.waitlight.asskicker.channel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;

import reactor.core.publisher.Mono;

/**
 * Shared helpers for IM bot channels (non-webhook): JSON helpers, recipients, HTTP calls.
 */
final class BotChannelSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WebClient webClient;

    BotChannelSupport(WebClient webClient) {
        this.webClient = webClient;
    }

    List<String> normalizeRecipients(UniAddress uniAddress, String providerName) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(providerName + " recipients required (chat/session id)");
        }
        return recipients;
    }

    String requireNonBlank(String value, String fieldName, String providerName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException(providerName + " spec requires " + fieldName);
        }
        return value.trim();
    }

    byte[] toJsonBytes(Map<String, Object> payload) throws Exception {
        return OBJECT_MAPPER.writeValueAsBytes(payload);
    }

    Mono<Map<String, Object>> postJson(String uri, byte[] bodyBytes, String providerName) {
        return postJson(uri, bodyBytes, Map.of(), providerName);
    }

    Mono<Map<String, Object>> postJson(String uri, byte[] bodyBytes, Map<String, String> headers,
            String providerName) {
        return webClient.post()
                .uri(uri)
                .headers(h -> headers.forEach(h::add))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyBytes)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> parseJsonMap(responseBody, providerName))
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        providerName + " " + ex.getStatusCode().value()
                                + (StringUtils.isNotBlank(ex.getResponseBodyAsString())
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    Mono<Map<String, Object>> getJson(String uri, String providerName) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> parseJsonMap(responseBody, providerName))
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        providerName + " " + ex.getStatusCode().value()
                                + (StringUtils.isNotBlank(ex.getResponseBodyAsString())
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }

    String buildUrlWithQuery(String baseUrl, MultiValueMap<String, String> queryParams) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParams(queryParams)
                .build(true)
                .toUriString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String responseBody, String providerName) {
        try {
            return OBJECT_MAPPER.readValue(responseBody, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(providerName + " invalid response: " + responseBody, e);
        }
    }

    static int intValue(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.isNumeric(s)) {
            return Integer.parseInt(s);
        }
        return defaultValue;
    }
}
