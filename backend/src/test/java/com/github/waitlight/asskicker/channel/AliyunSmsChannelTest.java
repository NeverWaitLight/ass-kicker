package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.test.StepVerifier;

class AliyunSmsChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private AliyunSmsChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String base = mockWebServer.url("/").toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String providerJson = String.format("""
                {
                  "code": "aliyun-sms-test",
                  "channelType": "SMS",
                  "providerType": "ALIYUN_SMS",
                  "enabled": true,
                  "properties": {
                    "accessKeyId": "test-ak",
                    "accessKeySecret": "test-sk",
                    "signName": "测试签名",
                    "templateCode": "SMS_TPL",
                    "endpoint": "%s"
                  }
                }
                """, base);
        ChannelEntity provider = MAPPER.readValue(providerJson, ChannelEntity.class);
        channel = new AliyunSmsChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_parsesOkResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json;charset=UTF-8")
                .setBody("{\"Code\":\"OK\",\"Message\":\"OK\",\"RequestId\":\"r1\"}"));

        UniMessage message = new UniMessage();
        message.setContent("ignored-for-template");
        message.setTemplateParams(Map.of("code", "1234"));
        UniAddress address = UniAddress.builder()
                .channelType(com.github.waitlight.asskicker.model.ChannelType.SMS)
                .channelProviderType(ChannelProviderType.ALIYUN_SMS)
                .recipients(java.util.Set.of("13800138000"))
                .build();

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("ALIYUN_SMS ok 1 recipient(s)")
                .verifyComplete();

        RecordedRequest req = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(req.getMethod()).isEqualTo("POST");
    }
}
