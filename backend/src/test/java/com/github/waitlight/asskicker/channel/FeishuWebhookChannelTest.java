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

class FeishuWebhookChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private FeishuWebhookChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String providerJson = String.format("""
                {
                  "code": "feishu-webhook-test",
                  "channelType": "IM",
                  "providerType": "FEISHU",
                  "enabled": true,
                  "properties": {
                    "url": "%s/open-apis/bot/v2/hook"
                  }
                }
                """, mockWebServer.url("/").toString().replaceAll("/$", ""));
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        channel = new FeishuWebhookChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
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
                .setBody("{\"code\":0,\"msg\":\"success\"}"));

        UniMessage message = new UniMessage();
        message.setTitle("标题");
        message.setContent("内容");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.FEISHU, "token-001");

        StepVerifier.create(channel.send(message, address))
                .expectNext("FEISHU ok 1 recipient(s)")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/open-apis/bot/v2/hook/token-001");
        JsonNode payload = MAPPER.readTree(request.getBody().readUtf8());
        assertThat(payload.get("msg_type").asText()).isEqualTo("text");
        assertThat(payload.get("content").get("text").asText()).contains("标题").contains("内容");
    }

    @Test
    void send_platformFailure_returnsMappedException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":19024,\"msg\":\"token expired\"}"));

        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.FEISHU, "bad-token");

        StepVerifier.create(channel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FEISHU platform failure"))
                .verify();
    }

    @Test
    void send_emptyRecipients_returnsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.FEISHU);

        StepVerifier.create(channel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("FEISHU recipients required"))
                .verify();
    }
}
