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

class WecomBotChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private WecomBotChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String base = mockWebServer.url("/").toString().replaceAll("/$", "");
        String providerJson = String.format("""
                {
                  "code": "wecom-bot-test",
                  "channelType": "IM",
                  "providerType": "WECOM_BOT",
                  "enabled": true,
                  "properties": {
                    "corpId": "ww_corp",
                    "corpSecret": "corp_secret",
                    "getTokenUrl": "%s/cgi-bin/gettoken",
                    "messageSendUrl": "%s/cgi-bin/appchat/send"
                  }
                }
                """, base, base);
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        channel = new WecomBotChannel(provider, WebClient.create());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_getTokenThenAppChat() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":0,\"access_token\":\"atok\",\"expires_in\":7200}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\"}"));

        UniMessage message = new UniMessage();
        message.setTitle("标题");
        message.setContent("正文");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.WECOM_BOT, "ck", "chatid_1");

        StepVerifier.create(channel.send(message, address))
                .expectNext("WECOM_BOT ok 1 chat(s)")
                .verifyComplete();

        RecordedRequest tokenReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(tokenReq.getMethod()).isEqualTo("GET");
        assertThat(tokenReq.getPath()).startsWith("/cgi-bin/gettoken");
        assertThat(tokenReq.getPath()).contains("corpid=ww_corp");
        assertThat(tokenReq.getPath()).contains("corpsecret=corp_secret");

        RecordedRequest sendReq = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(sendReq.getMethod()).isEqualTo("POST");
        assertThat(sendReq.getPath()).startsWith("/cgi-bin/appchat/send");
        assertThat(sendReq.getPath()).contains("access_token=atok");
        JsonNode body = MAPPER.readTree(sendReq.getBody().readUtf8());
        assertThat(body.get("chatid").asText()).isEqualTo("chatid_1");
        assertThat(body.get("msgtype").asText()).isEqualTo("text");
        assertThat(body.get("text").get("content").asText()).contains("标题");
    }

    @Test
    void send_appChatFailure_returnsMappedException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":0,\"access_token\":\"atok\"}"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"errcode\":86004,\"errmsg\":\"invalid chatid\"}"));

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.WECOM_BOT, "k", "bad");

        StepVerifier.create(channel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("WECOM_BOT platform failure"))
                .verify();
    }

    @Test
    void send_missingCorpId_returnsIllegalStateException() throws Exception {
        String providerJson = """
                {
                  "code": "wecom-bot-bad",
                  "channelType": "IM",
                  "providerType": "WECOM_BOT",
                  "enabled": true,
                  "properties": {
                    "corpId": "",
                    "corpSecret": "s"
                  }
                }
                """;
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        WecomBotChannel badChannel = new WecomBotChannel(provider, WebClient.create());

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.WECOM_BOT, "k", "cid");

        StepVerifier.create(badChannel.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("WECOM_BOT spec requires corpId"))
                .verify();
    }

    @Test
    void send_missingGetTokenUrl_returnsIllegalStateException() throws Exception {
        String providerJson = """
                {
                  "code": "wecom-bot-no-token-url",
                  "channelType": "IM",
                  "providerType": "WECOM_BOT",
                  "enabled": true,
                  "properties": {
                    "corpId": "c",
                    "corpSecret": "s",
                    "messageSendUrl": "https://example/cgi-bin/appchat/send"
                  }
                }
                """;
        ChannelProviderEntity provider = MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        WecomBotChannel ch = new WecomBotChannel(provider, WebClient.create());

        UniMessage message = new UniMessage();
        message.setContent("x");
        UniAddress address = UniAddress.ofImBot(ChannelProviderType.WECOM_BOT, "k", "cid");

        StepVerifier.create(ch.send(message, address))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("WECOM_BOT spec requires getTokenUrl"))
                .verify();
    }
}
