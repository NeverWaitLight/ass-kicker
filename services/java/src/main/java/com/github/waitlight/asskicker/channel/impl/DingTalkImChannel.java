package com.github.waitlight.asskicker.channel.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * DingTalk custom robot: sends text messages via webhook with HMAC-SHA256 signing.
 */
public class DingTalkImChannel extends Channel<ImReq> {

    public static final ChannelType TYPE = ChannelType.DINGTALK;
    public static final ChannelProvider PROVIDER = ChannelProvider.DINGTALK;

    private static final String WEBHOOK_URL = "https://oapi.dingtalk.com/robot/send";

    public DingTalkImChannel(ChannelEntity entity, WebClient webClient, ObjectMapper objectMapper) {
        super(entity, webClient, objectMapper);
    }

    @Override
    public Mono<String> send(ImReq req) {
        return Mono.fromCallable(() -> {
            String url = buildSignedUrl(req.getToken(), req.getSecret());
            DefaultDingTalkClient client = new DefaultDingTalkClient(url);

            OapiRobotSendRequest request = new OapiRobotSendRequest();
            request.setMsgtype("text");
            OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
            text.setContent(StringUtils.defaultString(req.getContent()));
            request.setText(text);

            OapiRobotSendResponse response = client.execute(request);
            if (response == null) {
                throw new IllegalStateException("DINGTALK_BOT empty response");
            }
            if (response.getErrcode() != null && response.getErrcode() != 0L) {
                throw new IllegalStateException(
                        "DINGTALK_BOT err code=" + response.getErrcode() + " msg=" + response.getErrmsg());
            }
            return "DINGTALK_BOT ok";
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static String buildSignedUrl(String token, String secret) {
        StringBuilder url = new StringBuilder(WEBHOOK_URL).append("?access_token=").append(token);
        if (StringUtils.isNotBlank(secret)) {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            String sign = sign(stringToSign, secret);
            url.append("&timestamp=").append(timestamp).append("&sign=").append(sign);
        }
        return url.toString();
    }

    private static String sign(String stringToSign, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("DINGTALK_BOT sign failed", e);
        }
    }
}
