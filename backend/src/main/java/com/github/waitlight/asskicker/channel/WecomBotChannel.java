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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WeCom app chat message to group chat by chat_id (appchat/send).
 */
public class WecomBotChannel extends Channel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String DEFAULT_APP_CHAT_SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/appchat/send";

    private final Spec spec;

    public WecomBotChannel(ChannelProviderEntity provider, WebClient webClient) {
        super(webClient);
        this.spec = WecomBotSpecMapper.INSTANCE.toSpec(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = normalizeRecipients(uniAddress, "WECOM_BOT");
            requireNonBlank(spec.corpId(), "corpId", "WECOM_BOT");
            requireNonBlank(spec.corpSecret(), "corpSecret", "WECOM_BOT");

            String getTokenUrl = StringUtils.defaultIfBlank(spec.getTokenUrl(), DEFAULT_GET_TOKEN_URL);
            String appChatSendUrl = StringUtils.defaultIfBlank(spec.messageSendUrl(), DEFAULT_APP_CHAT_SEND_URL);

            String text = buildPlainText(uniMessage);

            return fetchAccessToken(getTokenUrl)
                    .flatMap(token -> Flux.fromIterable(chatIds)
                            .concatMap(chatId -> sendAppChat(appChatSendUrl, token, chatId, text))
                            .collectList()
                            .map(ignore -> "WECOM_BOT ok " + chatIds.size() + " chat(s)"));
        });
    }

    private Mono<String> fetchAccessToken(String getTokenUrl) {
        LinkedMultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("corpid", spec.corpId().trim());
        q.add("corpsecret", spec.corpSecret().trim());
        String uri = buildUrlWithQuery(getTokenUrl, q);
        return getJson(uri, "WECOM_BOT")
                .flatMap(resp -> {
                    int err = intValue(resp.get("errcode"), -1);
                    if (err != 0) {
                        return Mono.error(new IllegalStateException(
                                "WECOM_BOT gettoken failure errcode=" + err + " errmsg="
                                        + String.valueOf(resp.get("errmsg"))));
                    }
                    Object token = resp.get("access_token");
                    if (token == null || StringUtils.isBlank(String.valueOf(token))) {
                        return Mono.error(new IllegalStateException("WECOM_BOT gettoken missing access_token"));
                    }
                    return Mono.just(String.valueOf(token));
                });
    }

    private Mono<String> sendAppChat(String appChatSendUrl, String accessToken, String chatId, String text) {
        LinkedMultiValueMap<String, String> q = new LinkedMultiValueMap<>();
        q.add("access_token", accessToken);
        String uri = buildUrlWithQuery(appChatSendUrl, q);

        Map<String, Object> textBody = new LinkedHashMap<>();
        textBody.put("content", text);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chatid", chatId);
        body.put("msgtype", "text");
        body.put("text", textBody);
        body.put("safe", 0);

        byte[] bytes;
        try {
            bytes = toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return postJson(uri, bytes, "WECOM_BOT")
                .flatMap(this::resolveSendResponse);
    }

    private Mono<String> resolveSendResponse(Map<String, Object> response) {
        int err = intValue(response.get("errcode"), -1);
        if (err != 0) {
            return Mono.error(new IllegalStateException(
                    "WECOM_BOT platform failure errcode=" + err + " errmsg="
                            + String.valueOf(response.get("errmsg"))));
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
        return OBJECT_MAPPER.writeValueAsBytes(payload);
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

    private Mono<Map<String, Object>> getJson(String uri, String providerName) {
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

    private String buildUrlWithQuery(String baseUrl, MultiValueMap<String, String> queryParams) {
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

    private static int intValue(Object value, int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && StringUtils.isNumeric(s)) {
            return Integer.parseInt(s);
        }
        return defaultValue;
    }

    private static String buildPlainText(UniMessage uniMessage) {
        String title = uniMessage != null ? uniMessage.getTitle() : null;
        String content = uniMessage != null ? uniMessage.getContent() : null;
        if (StringUtils.isNotBlank(title)) {
            return title + "\n" + StringUtils.defaultString(content);
        }
        return StringUtils.defaultString(content);
    }

    record Spec(
            String corpId,
            String corpSecret,
            String getTokenUrl,
            String messageSendUrl) {
    }
}

@Mapper
interface WecomBotSpecMapper {
    WecomBotSpecMapper INSTANCE = Mappers.getMapper(WecomBotSpecMapper.class);

    @Mapping(target = "corpId", source = "properties.corpId")
    @Mapping(target = "corpSecret", source = "properties.corpSecret")
    @Mapping(target = "getTokenUrl", source = "properties.getTokenUrl")
    @Mapping(target = "messageSendUrl", source = "properties.messageSendUrl")
    WecomBotChannel.Spec toSpec(Map<String, String> properties);
}
