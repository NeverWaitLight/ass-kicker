package com.github.waitlight.asskicker.sender.im;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DingTalkIMSenderTest {

    private MockWebServer mockWebServer;
    private DingTalkIMSender dingTalkIMSender;
    private DingTalkIMSenderConfig dingTalkConfig;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        dingTalkConfig = new DingTalkIMSenderConfig();
        dingTalkConfig.setWebhookUrl(mockWebServer.url("/webhook").toString());
        dingTalkConfig.setTimeout(Duration.ofSeconds(5));
        dingTalkConfig.setMaxRetries(3);
        dingTalkConfig.setRetryDelay(Duration.ofMillis(100));

        dingTalkIMSender = new DingTalkIMSender(dingTalkConfig);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSendMessageSuccessfully() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\",\"msgId\":\"msg-123\"}")
                .addHeader("Content-Type", "application/json"));

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("msg-123");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).contains("/webhook");
    }

    @Test
    void shouldReturnFailureWhenRequestIsNull() {
        MessageResponse response = dingTalkIMSender.send(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldRetryOnServerError() throws InterruptedException {
        // 钉钉 API 在 5xx 错误时会重试
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\",\"msgId\":\"msg-456\"}"));

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        // 最终应该成功
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void shouldFailAfterMaxRetries() {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        }

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SERVER_ERROR");
    }

    @Test
    void shouldCategorizeAuthenticationError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"errcode\":401,\"errmsg\":\"invalid token\"}"));

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void shouldCategorizeRateLimitError() {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(429)
                    .setBody("{\"errcode\":429,\"errmsg\":\"rate limit exceeded\"}"));
        }

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldIncludeSubjectInMessageContent() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\",\"msgId\":\"msg-789\"}"));

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("紧急告警")
                .content("数据库连接失败")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isTrue();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        // 检查请求体包含文本内容
        assertThat(requestBody).contains("紧急告警");
        assertThat(requestBody).contains("数据库连接失败");
    }

    @Test
    void shouldIncludeAttributesInRequestBody() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"errcode\":0,\"errmsg\":\"ok\",\"msgId\":\"msg-101\"}"));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("atMobiles", new String[]{"13800138000"});
        attributes.put("isAtAll", true);

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .attributes(attributes)
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        assertThat(response.isSuccess()).isTrue();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        // 检查 @ 功能
        assertThat(requestBody).contains("at");
        assertThat(requestBody).contains("13800138000");
    }

    @Test
    void shouldReturnFailureWhenDingTalkApiReturnsError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"errcode\":330000,\"errmsg\":\"invalid webhook\"}"));

        MessageRequest request = MessageRequest.builder()
                .recipient("user123")
                .subject("告警通知")
                .content("服务器 CPU 使用率超过 90%")
                .build();

        MessageResponse response = dingTalkIMSender.send(request);

        // 当钉钉 API 返回错误码时，应该返回失败
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("invalid webhook");
    }
}
