package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpApiEmailSenderTest {

    private MockWebServer mockWebServer;
    private HttpApiEmailSender httpApiEmailSender;
    private HttpApiEmailSenderProperty httpApiProperties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        httpApiProperties = new HttpApiEmailSenderProperty();
        httpApiProperties.setBaseUrl(mockWebServer.url("/").toString());
        httpApiProperties.setPath("/api/mail/send");
        httpApiProperties.setApiKeyHeader("Authorization");
        httpApiProperties.setApiKey("Bearer test-token");
        httpApiProperties.setFrom("notify@example.com");
        httpApiProperties.setTimeout(Duration.ofSeconds(5));
        httpApiProperties.setMaxRetries(3);
        httpApiProperties.setRetryDelay(Duration.ofMillis(100));

        WebClient.Builder webClientBuilder = WebClient.builder();
        httpApiEmailSender = new HttpApiEmailSender(webClientBuilder, httpApiProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSendEmailSuccessfully() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("message-id-123")
                .addHeader("Content-Type", "text/plain"));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("message-id-123");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/mail/send");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void shouldReturnFailureWhenRequestIsNull() {
        MessageResponse response = httpApiEmailSender.send(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldRetryOnServerError() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("message-id-456"));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldFailAfterMaxRetries() {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        }

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("SERVER_ERROR");
    }

    @Test
    void shouldCategorizeAuthenticationError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void shouldCategorizeRateLimitError() {
        for (int i = 0; i < 10; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        }

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void shouldIncludeAttributesInRequestBody() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("message-id-789"));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("priority", "high");
        attributes.put("tag", "notification");

        MessageRequest request = MessageRequest.builder()
                .recipient("recipient@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .attributes(attributes)
                .build();

        MessageResponse response = httpApiEmailSender.send(request);

        assertThat(response.isSuccess()).isTrue();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("priority");
        assertThat(requestBody).contains("high");
        assertThat(requestBody).contains("tag");
        assertThat(requestBody).contains("notification");
    }
}
