package com.github.waitlight.asskicker.channel;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Feishu/Lark custom app bot: tenant access token + im/v1/messages (chat_id).
 */
@ChannelImpl(providerType = ProviderType.FEISHU_BOT, propertyClass = FeishuBotChannel.Properties.class)
public class FeishuBotChannel extends Channel {

    private static final String DEFAULT_RECEIVE_ID_TYPE = "chat_id";

    private final Properties properties;

    public FeishuBotChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = FeishuBotSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = normalizeRecipients(uniAddress, "FEISHU_BOT");
            requireNonBlank(properties.appId(), "appId", "FEISHU_BOT");
            requireNonBlank(properties.appSecret(), "appSecret", "FEISHU_BOT");
            String tenantTokenUrl = requireNonBlank(properties.tenantTokenUrl(), "tenantTokenUrl", "FEISHU_BOT");
            String messageSendUrl = requireNonBlank(properties.messageSendUrl(), "messageSendUrl", "FEISHU_BOT");
            String receiveIdType = StringUtils.defaultIfBlank(properties.receiveIdType(), DEFAULT_RECEIVE_ID_TYPE);

            String text = buildPlainText(uniMessage);
            String contentJson;
            try {
                contentJson = objectMapper.writeValueAsString(Map.of("text", text));
            } catch (JsonProcessingException e) {
                return Mono.error(e);
            }

            return fetchTenantToken(tenantTokenUrl)
                    .flatMap(token -> Flux.fromIterable(chatIds)
                            .concatMap(chatId -> sendMessage(messageSendUrl, receiveIdType, token, chatId, contentJson))
                            .collectList()
                            .map(ignore -> "FEISHU_BOT ok " + chatIds.size() + " chat(s)"));
        });
    }

    private Mono<String> fetchTenantToken(String tenantTokenUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_id", properties.appId().trim());
        body.put("app_secret", properties.appSecret().trim());
        byte[] bytes;
        try {
            bytes = toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return postJson(tenantTokenUrl, bytes, "FEISHU_BOT")
                .flatMap(resp -> {
                    int code = intValue(resp.get("code"), -1);
                    if (code != 0) {
                        return Mono.error(new IllegalStateException(
                                "FEISHU_BOT token failure code=" + code + " msg=" + String.valueOf(resp.get("msg"))));
                    }
                    Object token = resp.get("tenant_access_token");
                    if (token == null || StringUtils.isBlank(String.valueOf(token))) {
                        return Mono.error(new IllegalStateException("FEISHU_BOT token response missing tenant_access_token"));
                    }
                    return Mono.just(String.valueOf(token));
                });
    }

    private Mono<String> sendMessage(String messageSendUrl, String receiveIdType, String tenantToken, String chatId,
            String contentJson) {
        String uri = messageSendUrl + "?receive_id_type=" + encodeQuery(receiveIdType);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("receive_id", chatId);
        body.put("msg_type", "text");
        body.put("content", contentJson);
        byte[] bytes;
        try {
            bytes = toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        Map<String, String> headers = Map.of("Authorization", "Bearer " + tenantToken);
        return postJson(uri, bytes, headers, "FEISHU_BOT")
                .flatMap(this::resolveSendResponse);
    }

    private Mono<String> resolveSendResponse(Map<String, Object> response) {
        int code = intValue(response.get("code"), -1);
        if (code != 0) {
            return Mono.error(new IllegalStateException(
                    "FEISHU_BOT platform failure code=" + code + " msg=" + String.valueOf(response.get("msg"))));
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

    private static String encodeQuery(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
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
            @NotBlank(message = "appId 不能为空") String appId,
            @NotBlank(message = "appSecret 不能为空") String appSecret,
            @NotBlank(message = "tenantTokenUrl 不能为空") String tenantTokenUrl,
            @NotBlank(message = "messageSendUrl 不能为空") String messageSendUrl,
            String receiveIdType) {
    }
}

@Mapper
interface FeishuBotSpecMapper {
    FeishuBotSpecMapper INSTANCE = Mappers.getMapper(FeishuBotSpecMapper.class);

    @Mapping(target = "appId", source = "properties.appId")
    @Mapping(target = "appSecret", source = "properties.appSecret")
    @Mapping(target = "tenantTokenUrl", source = "properties.tenantTokenUrl")
    @Mapping(target = "messageSendUrl", source = "properties.messageSendUrl")
    @Mapping(target = "receiveIdType", source = "properties.receiveIdType")
    FeishuBotChannel.Properties toSpec(Map<String, String> properties);
}
