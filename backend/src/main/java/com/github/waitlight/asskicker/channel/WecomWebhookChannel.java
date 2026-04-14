package com.github.waitlight.asskicker.channel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WecomWebhookChannel extends Channel {

    private final Spec spec;

    public WecomWebhookChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = WecomSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress, "WECOM");
            String baseUrl = requireBaseUrl(spec.url(), "WECOM");

            return Flux.fromIterable(recipients)
                    .concatMap(recipient -> {
                        String endpoint = buildQueryUrl(baseUrl, "key", recipient);
                        byte[] body;
                        try {
                            body = toJsonBytes(buildPayload(uniMessage));
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                        return postJson(endpoint, body, "WECOM")
                                .flatMap(this::resolveResponse);
                    })
                    .collect(Collectors.joining(","))
                    .map(ignore -> "WECOM ok " + recipients.size() + " recipient(s)");
        });
    }

    private Map<String, Object> buildPayload(UniMessage uniMessage) {
        String title = uniMessage != null ? uniMessage.getTitle() : null;
        String content = uniMessage != null ? uniMessage.getContent() : null;
        String text = StringUtils.isNotBlank(title) ? title + "\n" + StringUtils.defaultString(content)
                : StringUtils.defaultString(content);

        Map<String, Object> markdown = new LinkedHashMap<>();
        markdown.put("content", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);
        return payload;
    }

    private Mono<String> resolveResponse(Map<String, Object> response) {
        int errcode = intValue(response.get("errcode"), -1);
        if (errcode != 0) {
            return Mono.error(new IllegalStateException(
                    "WECOM platform failure errcode=" + errcode + " errmsg=" + String.valueOf(response.get("errmsg"))));
        }
        return Mono.just("ok");
    }

    private List<String> normalizeRecipients(UniAddress uniAddress, String providerName) {
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

    private String requireBaseUrl(String url, String providerName) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException(providerName + " spec requires url");
        }
        return url.trim();
    }

    private String buildQueryUrl(String baseUrl, String queryKey, String recipient) {
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        String encodedRecipient = URLEncoder.encode(recipient, StandardCharsets.UTF_8);
        return baseUrl + delimiter + queryKey + "=" + encodedRecipient;
    }

    private byte[] toJsonBytes(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsBytes(payload);
    }

    private Mono<Map<String, Object>> postJson(String endpoint, byte[] bodyBytes, String providerName) {
        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyBytes)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseBody -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = objectMapper.readValue(responseBody, Map.class);
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

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.isNumeric(s)) {
            return Integer.parseInt(s);
        }
        return defaultValue;
    }

    record Spec(String url) {
    }
}

@Mapper
interface WecomSpecMapper {
    WecomSpecMapper INSTANCE = Mappers.getMapper(WecomSpecMapper.class);

    @Mapping(target = "url", source = "properties.url")
    WecomWebhookChannel.Spec toSpec(Map<String, String> properties);
}
