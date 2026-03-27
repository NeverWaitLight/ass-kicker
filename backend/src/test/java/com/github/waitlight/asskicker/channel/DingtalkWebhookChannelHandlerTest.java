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

class DingtalkWebhookChannelHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private DingtalkWebhookChannelHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String providerJson = String.format("""
                {
                  "code": "dingtalk-webhook-test",
                  "channelType": "IM",
                  "providerType": "DINGTALK",
                  "enabled": true,
                  "properties": {
                    "url": "%s/robot/send"
                  }
                }
                """, mockWebServer.url("/").toString().replaceAll("/$", ""));
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        handler = new DingtalkWebhookChannelHandler(provider, WebClient.create());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_verifiesRequestBodyAndPath() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\"}"));

        UniMessage message = new UniMessage();
        message.setTitle("告警");
        message.setContent("服务异常");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.DINGTALK, "abc-token");

        StepVerifier.create(handler.send(message, address))
                .expectNext("DINGTALK ok 1 recipient(s)")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/robot/send?access_token=abc-token");
        JsonNode payload = MAPPER.readTree(request.getBody().readUtf8());
        assertThat(payload.get("msgtype").asText()).isEqualTo("markdown");
        assertThat(payload.get("markdown").get("title").asText()).isEqualTo("告警");
        assertThat(payload.get("markdown").get("text").asText()).contains("服务异常");
    }

    @Test
    void send_platformFailure_returnsMappedException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":310000,\"errmsg\":\"invalid token\"}"));

        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.DINGTALK, "bad-token");

        StepVerifier.create(handler.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("DINGTALK platform failure"))
                .verify();
    }

    @Test
    void send_emptyRecipients_returnsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.DINGTALK);

        StepVerifier.create(handler.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("DINGTALK recipients required"))
                .verify();
    }
}
