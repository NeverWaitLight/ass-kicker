package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
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

class WecomWebhookChannelHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_verifiesRequestBodyAndPath() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WecomWebhookChannelHandler handler = createHandler(mockWebServer.url("/").toString().replaceAll("/$", ""));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\"}"));

        UniMessage message = new UniMessage();
        message.setTitle("标题");
        message.setContent("正文");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.WECOM, "wecom-key");

        StepVerifier.create(handler.send(message, address))
                .expectNext("WECOM ok 1 recipient(s)")
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/cgi-bin/webhook/send?key=wecom-key");
        JsonNode payload = MAPPER.readTree(request.getBody().readUtf8());
        assertThat(payload.get("msgtype").asText()).isEqualTo("markdown");
        assertThat(payload.get("markdown").get("content").asText()).contains("标题").contains("正文");
    }

    @Test
    void send_platformFailure_returnsMappedException() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WecomWebhookChannelHandler handler = createHandler(mockWebServer.url("/").toString().replaceAll("/$", ""));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":93000,\"errmsg\":\"invalid key\"}"));

        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.WECOM, "bad-key");

        StepVerifier.create(handler.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("WECOM platform failure"))
                .verify();
    }

    @Test
    void send_missingUrlConfig_returnsIllegalStateException() throws Exception {
        String providerJson = """
                {
                  "code": "wecom-webhook-test",
                  "channelType": "IM",
                  "providerType": "WECOM",
                  "enabled": true,
                  "properties": {
                    "url": ""
                  }
                }
                """;
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        WecomWebhookChannelHandler handler = new WecomWebhookChannelHandler(provider, WebClient.create());

        UniMessage message = new UniMessage();
        message.setContent("test");
        UniAddress address = UniAddress.ofImWebhook(ChannelProviderType.WECOM, "wecom-key");

        StepVerifier.create(handler.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("WECOM spec requires url"))
                .verify();
    }

    private WecomWebhookChannelHandler createHandler(String baseUrl) throws Exception {
        String providerJson = String.format("""
                {
                  "code": "wecom-webhook-test",
                  "channelType": "IM",
                  "providerType": "WECOM",
                  "enabled": true,
                  "properties": {
                    "url": "%s/cgi-bin/webhook/send"
                  }
                }
                """, baseUrl);
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        return new WecomWebhookChannelHandler(provider, WebClient.create());
    }
}
