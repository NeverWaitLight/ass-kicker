package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import com.github.waitlight.asskicker.model.ChannelEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ProviderType;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.test.StepVerifier;

class FeishuBotChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private FeishuBotChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String base = mockWebServer.url("/").toString().replaceAll("/$", "");
        String providerJson = String.format("""
                {
                  "code": "feishu-bot-test",
                  "channelType": "IM",
                  "providerType": "FEISHU_BOT",
                  "enabled": true,
                  "properties": {
                    "appId": "cli_xxx",
                    "appSecret": "sec_xxx",
                    "tenantTokenUrl": "%s/open-apis/auth/v3/tenant_access_token/internal",
                    "messageSendUrl": "%s/open-apis/im/v1/messages"
                  }
                }
                """, base, base);
        ChannelEntity provider = MAPPER.readValue(providerJson, ChannelEntity.class);
        channel = new FeishuBotChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_tokenThenMessage() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"tenant_access_token\":\"t-feishu\",\"expire\":7200}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"data\":{\"message_id\":\"om_1\"}}"));

        UniMessage message = new UniMessage();
        message.setTitle("标题");
        message.setContent("内容");
        UniAddress address = UniAddress.ofImBot(ProviderType.FEISHU_BOT, "ck", "oc_chat_1");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("FEISHU_BOT ok 1 chat(s)")
                .verifyComplete();

        RecordedRequest tokenReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(tokenReq.getPath()).isEqualTo("/open-apis/auth/v3/tenant_access_token/internal");

        RecordedRequest msgReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(msgReq.getPath()).startsWith("/open-apis/im/v1/messages");
        assertThat(msgReq.getPath()).contains("receive_id_type=chat_id");
        assertThat(msgReq.getHeader("Authorization")).isEqualTo("Bearer t-feishu");
        JsonNode body = MAPPER.readTree(msgReq.getBody().readUtf8());
        assertThat(body.get("receive_id").asText()).isEqualTo("oc_chat_1");
        assertThat(body.get("msg_type").asText()).isEqualTo("text");
        assertThat(body.get("content").asText()).contains("标题");
    }

    @Test
    void send_messageFailure_returnsMappedException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":0,\"tenant_access_token\":\"t\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":19002,\"msg\":\"fail\"}"));

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.FEISHU_BOT, "k", "oc");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FEISHU_BOT platform failure"))
                .verify();
    }

    @Test
    void send_emptyRecipients_returnsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.FEISHU_BOT, "k");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("FEISHU_BOT recipients required"))
                .verify();
    }

    @Test
    void send_missingTenantTokenUrl_returnsIllegalStateException() throws Exception {
        String providerJson = """
                {
                  "code": "feishu-bot-no-token-url",
                  "channelType": "IM",
                  "providerType": "FEISHU_BOT",
                  "enabled": true,
                  "properties": {
                    "appId": "cli_x",
                    "appSecret": "sec_x",
                    "messageSendUrl": "https://example/open-apis/im/v1/messages"
                  }
                }
                """;
        ChannelEntity provider = MAPPER.readValue(providerJson, ChannelEntity.class);
        FeishuBotChannel ch = new FeishuBotChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ProviderType.FEISHU_BOT, "k", "oc");

        StepVerifier.create(ch.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FEISHU_BOT spec requires tenantTokenUrl"))
                .verify();
    }
}
