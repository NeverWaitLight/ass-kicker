package com.github.waitlight.asskicker.channel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * WeCom app chat message to group chat by chat_id (appchat/send).
 */
public class WecomBotChannel extends Channel {

    private static final String DEFAULT_GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String DEFAULT_APP_CHAT_SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/appchat/send";

    private final Spec spec;
    private final BotChannelSupport botSupport;

    public WecomBotChannel(ChannelProviderEntity provider, WebClient webClient) {
        super(webClient);
        this.spec = WecomBotSpecMapper.INSTANCE.toSpec(provider.getProperties());
        this.botSupport = new BotChannelSupport(webClient);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = botSupport.normalizeRecipients(uniAddress, "WECOM_BOT");
            botSupport.requireNonBlank(spec.corpId(), "corpId", "WECOM_BOT");
            botSupport.requireNonBlank(spec.corpSecret(), "corpSecret", "WECOM_BOT");

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
        String uri = botSupport.buildUrlWithQuery(getTokenUrl, q);
        return botSupport.getJson(uri, "WECOM_BOT")
                .flatMap(resp -> {
                    int err = BotChannelSupport.intValue(resp.get("errcode"), -1);
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
        String uri = botSupport.buildUrlWithQuery(appChatSendUrl, q);

        Map<String, Object> textBody = new LinkedHashMap<>();
        textBody.put("content", text);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chatid", chatId);
        body.put("msgtype", "text");
        body.put("text", textBody);
        body.put("safe", 0);

        byte[] bytes;
        try {
            bytes = botSupport.toJsonBytes(body);
        } catch (Exception e) {
            return Mono.error(e);
        }
        return botSupport.postJson(uri, bytes, "WECOM_BOT")
                .flatMap(this::resolveSendResponse);
    }

    private Mono<String> resolveSendResponse(Map<String, Object> response) {
        int err = BotChannelSupport.intValue(response.get("errcode"), -1);
        if (err != 0) {
            return Mono.error(new IllegalStateException(
                    "WECOM_BOT platform failure errcode=" + err + " errmsg="
                            + String.valueOf(response.get("errmsg"))));
        }
        return Mono.just("ok");
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
