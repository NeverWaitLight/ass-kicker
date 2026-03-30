package com.github.waitlight.asskicker.channel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Feishu/Lark custom app bot: tenant access token + im/v1/messages (chat_id).
 */
public class FeishuBotChannel extends Channel {

    private static final String DEFAULT_TENANT_TOKEN_URL =
            "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String DEFAULT_MESSAGE_SEND_URL = "https://open.feishu.cn/open-apis/im/v1/messages";
    private static final String DEFAULT_RECEIVE_ID_TYPE = "chat_id";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Spec spec;
    private final BotChannelSupport botSupport;

    public FeishuBotChannel(ChannelProviderEntity provider, WebClient webClient) {
        super(webClient);
        this.spec = FeishuBotSpecMapper.INSTANCE.toSpec(provider.getProperties());
        this.botSupport = new BotChannelSupport(webClient);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = botSupport.normalizeRecipients(uniAddress, "FEISHU_BOT");
            botSupport.requireNonBlank(spec.appId(), "appId", "FEISHU_BOT");
            botSupport.requireNonBlank(spec.appSecret(), "appSecret", "FEISHU_BOT");

            String tenantTokenUrl = StringUtils.defaultIfBlank(spec.tenantTokenUrl(), DEFAULT_TENANT_TOKEN_URL);
            String messageSendUrl = StringUtils.defaultIfBlank(spec.messageSendUrl(), DEFAULT_MESSAGE_SEND_URL);
            String receiveIdType = StringUtils.defaultIfBlank(spec.receiveIdType(), DEFAULT_RECEIVE_ID_TYPE);

            String text = buildPlainText(uniMessage);
            String contentJson;
            try {
                contentJson = MAPPER.writeValueAsString(Map.of("text", text));
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
        body.put("app_id", spec.appId().trim());
        body.put("app_secret", spec.appSecret().trim());
        byte[] bytes;
        try {
            bytes = botSupport.toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return botSupport.postJson(tenantTokenUrl, bytes, "FEISHU_BOT")
                .flatMap(resp -> {
                    int code = BotChannelSupport.intValue(resp.get("code"), -1);
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
            bytes = botSupport.toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        Map<String, String> headers = Map.of("Authorization", "Bearer " + tenantToken);
        return botSupport.postJson(uri, bytes, headers, "FEISHU_BOT")
                .flatMap(this::resolveSendResponse);
    }

    private Mono<String> resolveSendResponse(Map<String, Object> response) {
        int code = BotChannelSupport.intValue(response.get("code"), -1);
        if (code != 0) {
            return Mono.error(new IllegalStateException(
                    "FEISHU_BOT platform failure code=" + code + " msg=" + String.valueOf(response.get("msg"))));
        }
        return Mono.just("ok");
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

    record Spec(
            String appId,
            String appSecret,
            String tenantTokenUrl,
            String messageSendUrl,
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
    FeishuBotChannel.Spec toSpec(Map<String, String> properties);
}
