package com.github.waitlight.asskicker.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
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
import com.github.waitlight.asskicker.model.ChannelType;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.test.StepVerifier;

class AwsSnsSmsChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MockWebServer mockWebServer;
    private AwsSnsSmsChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String endpoint = mockWebServer.url("/").toString();
        String providerJson = String.format("""
                {
                  "code": "aws-sns-test",
                  "channelType": "SMS",
                  "providerType": "AWS_SMS",
                  "enabled": true,
                  "properties": {
                    "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
                    "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                    "region": "us-east-1",
                    "endpoint": "%s"
                  }
                }
                """, endpoint);
        ChannelEntity provider = MAPPER.readValue(providerJson, ChannelEntity.class);
        channel = new AwsSnsSmsChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    void send_success_postsSignedRequest() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/xml")
                .setBody("""
                        <PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
                          <PublishResult>
                            <MessageId>msg-1</MessageId>
                          </PublishResult>
                        </PublishResponse>
                        """));

        UniMessage message = new UniMessage();
        message.setContent("hello sns");
        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.SMS)
                .channelProviderType(ChannelProviderType.AWS_SMS)
                .recipients(java.util.Set.of("+12065550100"))
                .build();

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("AWS_SMS ok 1 recipient(s)")
                .verifyComplete();

        RecordedRequest req = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(req.getMethod()).isEqualTo("POST");
        String body = req.getBody().readString(StandardCharsets.UTF_8);
        assertThat(body).contains("Action=Publish");
        assertThat(body).contains("PhoneNumber=");
        assertThat(req.getHeader("Authorization")).startsWith("AWS4-HMAC-SHA256");
    }
}
