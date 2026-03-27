package com.github.waitlight.asskicker.channel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;

import reactor.core.publisher.Mono;

final class WebhookChannelSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final WebClient webClient;

    WebhookChannelSupport(WebClient webClient) {
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
            throw new IllegalArgumentException(providerName + " recipients required");
        }
        return recipients;
    }

    String requireBaseUrl(String url, String providerName) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException(providerName + " spec requires url");
        }
        return url.trim();
    }

    String buildQueryUrl(String baseUrl, String queryKey, String recipient) {
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        String encodedRecipient = URLEncoder.encode(recipient, StandardCharsets.UTF_8);
        return baseUrl + delimiter + queryKey + "=" + encodedRecipient;
    }

    String buildPathUrl(String baseUrl, String recipient) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String encodedRecipient = URLEncoder.encode(recipient, StandardCharsets.UTF_8);
        return normalizedBaseUrl + "/" + encodedRecipient;
    }

    byte[] toJsonBytes(Map<String, Object> payload) throws Exception {
        return OBJECT_MAPPER.writeValueAsBytes(payload);
    }

    Mono<Map<String, Object>> postJson(String endpoint, byte[] bodyBytes, String providerName) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyBytes)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = OBJECT_MAPPER.readValue(responseBody, Map.class);
                        return map;
                    } catch (Exception e) {
                        throw new IllegalStateException(providerName + " invalid response: " + responseBody, e);
                    }
                })
                .onErrorMap(WebClientResponseException.class, ex -> new IllegalStateException(
                        providerName + " " + ex.getStatusCode().value()
                                + (StringUtils.isNotBlank(ex.getResponseBodyAsString())
                                        ? ": " + ex.getResponseBodyAsString()
                                        : ""),
                        ex));
    }
}
