package com.github.waitlight.asskicker.channel.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.model.ChannelEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenRequest;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponse;
import com.aliyun.dingtalkoauth2_1_0.models.GetAccessTokenResponseBody;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendHeaders;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendRequest;
import com.aliyun.dingtalkrobot_1_0.models.OrgGroupSendResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * DingTalk enterprise robot: group messages via the official Open API SDK
 * ({@code com.aliyun:dingtalk}), which encapsulates OAuth token retrieval +
 * group-send under {@code api.dingtalk.com}.
 */
public class DingtalkBotChannel extends Channel<SendReq> {

    private static final String MSG_KEY_SAMPLE_TEXT = "sampleText";

    private final Properties properties;
    private final com.aliyun.dingtalkoauth2_1_0.Client oauthClient;
    private final com.aliyun.dingtalkrobot_1_0.Client robotClient;

    public DingtalkBotChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        try {
            this.oauthClient = new com.aliyun.dingtalkoauth2_1_0.Client(buildConfig());
            this.robotClient = new com.aliyun.dingtalkrobot_1_0.Client(buildConfig());
        } catch (Exception e) {
            throw new IllegalStateException("DINGTALK_BOT SDK client init failed", e);
        }
    }

    DingtalkBotChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper,
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient,
            com.aliyun.dingtalkrobot_1_0.Client robotClient) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.oauthClient = oauthClient;
        this.robotClient = robotClient;
    }

    /**
     * Visible for testing — lets tests inject mocked SDK clients to avoid real network calls.
     */
    public static DingtalkBotChannel forTesting(ChannelEntity provider, WebClient webClient,
            ObjectMapper objectMapper,
            com.aliyun.dingtalkoauth2_1_0.Client oauthClient,
            com.aliyun.dingtalkrobot_1_0.Client robotClient) {
        return new DingtalkBotChannel(provider, webClient, objectMapper, oauthClient, robotClient);
    }

    private static Config buildConfig() {
        Config config = new Config();
        config.setProtocol("https");
        config.setRegionId("central");
        return config;
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> chatIds = normalizeRecipients(uniAddress);
            requireNonBlank(properties.getAppKey(), "appKey");
            requireNonBlank(properties.getAppSecret(), "appSecret");
            requireNonBlank(properties.getRobotCode(), "robotCode");

            String msgParamJson;
            try {
                msgParamJson = objectMapper.writeValueAsString(Map.of("content", buildPlainText(uniMessage)));
            } catch (Exception e) {
                return Mono.error(e);
            }

            return Mono.fromCallable(() -> {
                String token = fetchAccessToken();
                for (String openConversationId : chatIds) {
                    sendGroupMessage(token, openConversationId, msgParamJson);
                }
                return "DINGTALK_BOT ok " + chatIds.size() + " chat(s)";
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private String fetchAccessToken() {
        GetAccessTokenRequest req = new GetAccessTokenRequest()
                .setAppKey(properties.getAppKey().trim())
                .setAppSecret(properties.getAppSecret().trim());
        try {
            GetAccessTokenResponse response = oauthClient.getAccessToken(req);
            GetAccessTokenResponseBody body = response != null ? response.getBody() : null;
            String token = body != null ? body.getAccessToken() : null;
            if (StringUtils.isBlank(token)) {
                throw new IllegalStateException("DINGTALK_BOT token response missing accessToken");
            }
            return token;
        } catch (TeaException te) {
            throw new IllegalStateException(
                    "DINGTALK_BOT platform failure code=" + te.getCode() + " message=" + te.getMessage(), te);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("DINGTALK_BOT getAccessToken failed: " + e.getMessage(), e);
        }
    }

    private void sendGroupMessage(String accessToken, String openConversationId, String msgParamJson) {
        OrgGroupSendRequest req = new OrgGroupSendRequest()
                .setRobotCode(properties.getRobotCode().trim())
                .setOpenConversationId(openConversationId)
                .setMsgKey(MSG_KEY_SAMPLE_TEXT)
                .setMsgParam(msgParamJson);
        OrgGroupSendHeaders headers = new OrgGroupSendHeaders();
        headers.setXAcsDingtalkAccessToken(accessToken);
        try {
            OrgGroupSendResponse response = robotClient.orgGroupSendWithOptions(req, headers, new RuntimeOptions());
            if (response == null || response.getBody() == null) {
                throw new IllegalStateException("DINGTALK_BOT empty group send response");
            }
        } catch (TeaException te) {
            throw new IllegalStateException(
                    "DINGTALK_BOT platform failure code=" + te.getCode() + " message=" + te.getMessage(), te);
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("DINGTALK_BOT orgGroupSend failed: " + e.getMessage(), e);
        }
    }

    private List<String> normalizeRecipients(UniAddress uniAddress) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("DINGTALK_BOT recipients required (chat/session id)");
        }
        return recipients;
    }

    private void requireNonBlank(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException("DINGTALK_BOT spec requires " + fieldName);
        }
    }

    private static String buildPlainText(UniMessage uniMessage) {
        String title = uniMessage != null ? uniMessage.getTitle() : null;
        String content = uniMessage != null ? uniMessage.getContent() : null;
        if (StringUtils.isNotBlank(title)) {
            return title + "\n" + StringUtils.defaultString(content);
        }
        return StringUtils.defaultString(content);
    }

    @Schema(description = "钉钉企业机器人通道配置")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        @Schema(description = "AppKey", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String appKey;

        @Schema(description = "AppSecret", requiredMode = Schema.RequiredMode.REQUIRED, type = "password")
        @NotBlank
        private String appSecret;

        @Schema(description = "RobotCode", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String robotCode;
    }
}
