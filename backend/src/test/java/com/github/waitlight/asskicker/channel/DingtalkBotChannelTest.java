package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.test.StepVerifier;

class DingtalkBotChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private DingtalkBotChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String base = mockWebServer.url("/").toString().replaceAll("/$", "");
        String providerJson = String.format("""
                {
                  "code": "dingtalk-bot-test",
                  "channelType": "IM",
                  "providerType": "DINGTALK_BOT",
                  "enabled": true,
                  "properties": {
                    "appKey": "app-key-1",
                    "appSecret": "app-secret-1",
                    "robotCode": "robot-code-1",
                    "accessTokenUrl": "%s/v1.0/oauth2/accessToken",
                    "groupSendUrl": "%s/v1.0/robot/groupMessages/send"
                  }
                }
                """, base, base);
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        channel = new DingtalkBotChannel(provider, WebClient.create());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_tokenThenGroupMessage() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"accessToken\":\"tok-abc\",\"expiresIn\":7200}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"processQueryKey\":\"pqk\"}"));

        UniMessage message = new UniMessage();
        message.setTitle("告警");
        message.setContent("服务异常");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.DINGTALK_BOT, "ch-1", "cid-open-1");

        StepVerifier.create(channel.send(message, address))
                .expectNext("DINGTALK_BOT ok 1 chat(s)")
                .verifyComplete();

        RecordedRequest tokenReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(tokenReq.getPath()).isEqualTo("/v1.0/oauth2/accessToken");
        JsonNode tokenBody = MAPPER.readTree(tokenReq.getBody().readUtf8());
        assertThat(tokenBody.get("appKey").asText()).isEqualTo("app-key-1");

        RecordedRequest sendReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(sendReq.getPath()).isEqualTo("/v1.0/robot/groupMessages/send");
        assertThat(sendReq.getHeader("x-acs-dingtalk-access-token")).isEqualTo("tok-abc");
        JsonNode sendBody = MAPPER.readTree(sendReq.getBody().readUtf8());
        assertThat(sendBody.get("robotCode").asText()).isEqualTo("robot-code-1");
        assertThat(sendBody.get("openConversationId").asText()).isEqualTo("cid-open-1");
        assertThat(sendBody.get("msgKey").asText()).isEqualTo("sampleText");
        assertThat(sendBody.get("msgParam").asText()).contains("服务异常");
    }

    @Test
    void send_groupFailure_returnsMappedException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"accessToken\":\"tok-abc\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":400,\"errmsg\":\"bad\"}"));

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.DINGTALK_BOT, "k", "cid");

        StepVerifier.create(channel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("DINGTALK_BOT platform failure"))
                .verify();
    }

    @Test
    void send_emptyRecipients_returnsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.DINGTALK_BOT, "k");

        StepVerifier.create(channel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("DINGTALK_BOT recipients required"))
                .verify();
    }
}
