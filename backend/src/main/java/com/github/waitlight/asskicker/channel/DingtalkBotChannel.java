package com.github.waitlight.asskicker.channel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * DingTalk enterprise robot: group messages via Open API (OAuth access token + group send).
 */
@ChannelImpl(providerType = ProviderType.DINGTALK_BOT, propertyClass = DingtalkBotChannel.Properties.class)
public class DingtalkBotChannel extends Channel {

    private static final String HEADER_ACCESS_TOKEN = "x-acs-dingtalk-access-token";
    private static final String MSG_KEY_SAMPLE_TEXT = "sampleText";

    private final Properties properties;

    public DingtalkBotChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = normalizeRecipients(uniAddress, "DINGTALK_BOT");
            requireNonBlank(properties.appKey(), "appKey", "DINGTALK_BOT");
            requireNonBlank(properties.appSecret(), "appSecret", "DINGTALK_BOT");
            requireNonBlank(properties.robotCode(), "robotCode", "DINGTALK_BOT");
            String accessTokenUrl = requireNonBlank(properties.accessTokenUrl(), "accessTokenUrl", "DINGTALK_BOT");
            String groupSendUrl = requireNonBlank(properties.groupSendUrl(), "groupSendUrl", "DINGTALK_BOT");

            String text = buildPlainText(uniMessage);
            String msgParamJson;
            try {
                msgParamJson = objectMapper.writeValueAsString(Map.of("content", text));
            } catch (JsonProcessingException e) {
                return Mono.error(e);
            }

            return fetchAccessToken(accessTokenUrl)
                    .flatMap(token -> Flux.fromIterable(chatIds)
                            .concatMap(openConversationId -> sendGroupMessage(groupSendUrl, token, openConversationId,
                                    msgParamJson))
                            .collectList()
                            .map(ignore -> "DINGTALK_BOT ok " + chatIds.size() + " chat(s)"));
        });
    }

    private Mono<String> fetchAccessToken(String accessTokenUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appKey", properties.appKey().trim());
        body.put("appSecret", properties.appSecret().trim());
        byte[] bytes;
        try {
            bytes = toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return postJson(accessTokenUrl, bytes, "DINGTALK_BOT")
                .flatMap(resp -> {
                    String token = stringOrNull(resp.get("accessToken"));
                    if (StringUtils.isBlank(token)) {
                        token = stringOrNull(resp.get("access_token"));
                    }
                    if (StringUtils.isBlank(token)) {
                        return Mono.error(new IllegalStateException(
                                "DINGTALK_BOT token response missing accessToken"));
                    }
                    return Mono.just(token);
                });
    }

    private Mono<String> sendGroupMessage(String groupSendUrl, String accessToken, String openConversationId,
            String msgParamJson) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("robotCode", properties.robotCode().trim());
        body.put("openConversationId", openConversationId);
        body.put("msgKey", MSG_KEY_SAMPLE_TEXT);
        body.put("msgParam", msgParamJson);
        byte[] bytes;
        try {
            bytes = toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        Map<String, String> headers = Map.of(HEADER_ACCESS_TOKEN, accessToken);
        return postJson(groupSendUrl, bytes, headers, "DINGTALK_BOT")
                .flatMap(this::resolveGroupSendResponse);
    }

    private Mono<String> resolveGroupSendResponse(Map<String, Object> response) {
        if (response.containsKey("errcode")) {
            int err = intValue(response.get("errcode"), -1);
            if (err != 0) {
                return Mono.error(new IllegalStateException(
                        "DINGTALK_BOT platform failure errcode=" + err + " errmsg="
                                + String.valueOf(response.get("errmsg"))));
            }
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
            throw new IllegalArgumentException(providerName + " recipients required (chat/session id)");
        }
        return recipients;
    }

    private String requireNonBlank(String value, String fieldName, String providerName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException(providerName + " spec requires " + fieldName);
        }
        return value.trim();
    }

    private byte[] toJsonBytes(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsBytes(payload);
    }

    private Mono<Map<String, Object>> postJson(String uri, byte[] bodyBytes, String providerName) {
        return postJson(uri, bodyBytes, Map.of(), providerName);
    }

    private Mono<Map<String, Object>> postJson(String uri, byte[] bodyBytes, Map<String, String> headers,
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String responseBody, String providerName) {
        try {
            return objectMapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(providerName + " invalid response: " + responseBody, e);
        }
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

    private static String stringOrNull(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String buildPlainText(UniMessage uniMessage) {
        String title = uniMessage != null ? uniMessage.getTitle() : null;
        String content = uniMessage != null ? uniMessage.getContent() : null;
        if (StringUtils.isNotBlank(title)) {
            return title + "\n" + StringUtils.defaultString(content);
        }
        return StringUtils.defaultString(content);
    }

    record Properties(
            @NotBlank(message = "appKey 不能为空") String appKey,
            @NotBlank(message = "appSecret 不能为空") String appSecret,
            @NotBlank(message = "robotCode 不能为空") String robotCode,
            @NotBlank(message = "accessTokenUrl 不能为空") String accessTokenUrl,
            @NotBlank(message = "groupSendUrl 不能为空") String groupSendUrl) {
    }
}
