package com.github.waitlight.asskicker.channel.impl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.exception.SendException;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Feishu (Lark) custom robot: sends text messages via webhook with optional HMAC-SHA256 signing.
 *
 * Feishu 官方 SDK (com.larksuite.oapi:larksuite-oapi) 面向企业应用 (需 AppID/AppSecret 走 OAuth),
 * 并未提供自定义机器人 webhook 的封装类,因此这里直接使用 WebClient 调用 webhook。
 */
@Channel(type = ChannelType.FEISHU, provider = ChannelProvider.FEISHU, reqType = ImReq.class)
public class FeiShuImChannel extends AbstractChannel<ImReq> {

    private static final String WEBHOOK_URL_TEMPLATE = "https://open.feishu.cn/open-apis/bot/v2/hook/%s";

    public FeiShuImChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        super(entity, webClient, objectMapper);
    }

    @Override
    protected Mono<String> doSend(ImReq req) {
        return Mono.defer(() -> {
            if (StringUtils.isBlank(req.getToken())) {
                return Mono.error(new IllegalArgumentException("FEISHU_BOT token required"));
            }

            String url = String.format(WEBHOOK_URL_TEMPLATE, req.getToken());
            Map<String, Object> body = buildBody(req);

            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .flatMap(FeiShuImChannel::verifyResponse);
        });
    }

    @Override
    public void dispose() {
    }

    private static Map<String, Object> buildBody(ImReq req) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(req.getSecret())) {
            long timestamp = System.currentTimeMillis() / 1000L;
            body.put("timestamp", String.valueOf(timestamp));
            body.put("sign", sign(timestamp, req.getSecret()));
        }
        body.put("msg_type", "text");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("text", StringUtils.defaultString(req.getContent()));
        body.put("content", content);
        return body;
    }

    private static String sign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[0]);
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            throw new IllegalStateException("FEISHU_BOT sign failed", e);
        }
    }

    @SuppressWarnings("rawtypes")
    private static Mono<String> verifyResponse(Map response) {
        Object codeObj = response.get("code");
        long code = codeObj instanceof Number ? ((Number) codeObj).longValue() : -1L;
        if (code != 0L) {
            String msg = String.valueOf(response.get("msg"));
            return Mono.error(new SendException("FEISHU_BOT err code=" + code + " msg=" + msg));
        }
        return Mono.just("FEISHU_BOT ok");
    }
}
