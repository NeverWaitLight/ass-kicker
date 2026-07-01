package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.impl.FcmPushChannel;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for FcmPushChannel using MockWebServer.
 *
 * <p>The channel now uses GoogleCredentials for auto token refresh. Tests inject a
 * pre-baked AccessToken via a package-private constructor so MockWebServer can still
 * intercept the actual FCM REST call without needing a real service account JSON.
 */
class FcmPushChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROJECT_ID = "test-project-12345";
    private static final String ACCESS_TOKEN = "ya29.test-access-token";

    private static final String DEVICE_TOKEN_1 = "fGx1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7a8b9c0d";
    private static final String DEVICE_TOKEN_2 = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0011223344556";

    private FcmMockServer mockServer;
    private FcmPushChannel channel;

    private static ChannelEntity createProvider() throws Exception {
        String providerJson = String.format("""
                {
                  "name": "FCM Mock Test",
                  "code": "fcm-mock-test",
                  "type": "PUSH",
                  "provider": "GOOGLE",
                  "providerType": "FCM",
                  "description": "FcmPushChannel test with MockWebServer",
                  "enabled": true,
                  "properties": {
                    "projectId": "%s",
                    "serviceAccountJson": "test-injected"
                  }
                }
                """, PROJECT_ID);
        return MAPPER.readValue(providerJson, ChannelEntity.class);
    }

    private static GoogleCredentials fakeCredentials() {
        AccessToken token = new AccessToken(ACCESS_TOKEN, new Date(System.currentTimeMillis() + 3_600_000L));
        return GoogleCredentials.create(token);
    }

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new FcmMockServer(PROJECT_ID);
        mockServer.start();

        String endpoint = mockServer.getBaseUrl() + "/v1/projects/" + PROJECT_ID + "/messages:send";
        ChannelEntity provider = createProvider();
        channel = FcmPushChannel.forTesting(provider, WebClient.create(),
                ChannelTestObjectMappers.channelObjectMapper(), fakeCredentials(), endpoint);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    // ==================== Success Scenarios ====================

    @Test
    void send_singleDevice_returnsSuccessWithName() throws Exception {
        String messageId = UUID.randomUUID().toString();
        mockServer.enqueueSuccess(messageId);

        UniMessage message = new UniMessage();
        message.setTitle("Test Title");
        message.setContent("Test Content");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        String expectedName = "projects/" + PROJECT_ID + "/messages/" + messageId;

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("FCM ok 1 device(s) name=" + expectedName)
                .verifyComplete();

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).contains("/v1/projects/" + PROJECT_ID + "/messages:send");
        mockServer.verifyRequestHeaders(request);
        mockServer.verifyRequestBody(request, DEVICE_TOKEN_1, "Test Title", "Test Content");
    }

    @Test
    void send_multipleDevices_returnsSuccessWithMultipleNames() throws Exception {
        String messageId1 = UUID.randomUUID().toString();
        String messageId2 = UUID.randomUUID().toString();
        mockServer.enqueueSuccess(messageId1);
        mockServer.enqueueSuccess(messageId2);

        UniMessage message = new UniMessage();
        message.setTitle("Multi Device Test");
        message.setContent("Testing multiple devices");
        UniAddress address = UniAddress.ofPush(
                ProviderType.FCM,
                DEVICE_TOKEN_1, DEVICE_TOKEN_2);

        String name1 = "projects/" + PROJECT_ID + "/messages/" + messageId1;
        String name2 = "projects/" + PROJECT_ID + "/messages/" + messageId2;

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .assertNext(result -> {
                    assertThat(result).startsWith("FCM ok 2 device(s) name=");
                    assertThat(result).contains(name1);
                    assertThat(result).contains(name2);
                })
                .verifyComplete();

        assertThat(mockServer.getRequestCount()).isEqualTo(2);

        RecordedRequest request1 = mockServer.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest request2 = mockServer.takeRequest(5, TimeUnit.SECONDS);

        Assertions.assertNotNull(request1);
        Assertions.assertNotNull(request2);

        mockServer.verifyRequestHeaders(request1);
        mockServer.verifyRequestHeaders(request2);
    }

    @Test
    void send_withoutTitle_sendsPayloadWithBodyOnly() throws Exception {
        String messageId = UUID.randomUUID().toString();
        mockServer.enqueueSuccess(messageId);

        UniMessage message = new UniMessage();
        message.setContent("Content without title");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .assertNext(result -> assertThat(result).startsWith("FCM ok 1 device(s) name="))
                .verifyComplete();

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        mockServer.verifyRequestBody(request, DEVICE_TOKEN_1, null, "Content without title");
    }

    @Test
    void send_autoGeneratesMessageName_fromResponse() throws Exception {
        mockServer.enqueueSuccess(null);

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .assertNext(result -> {
                    assertThat(result).startsWith("FCM ok 1 device(s) name=");
                    assertThat(result).contains("projects/" + PROJECT_ID + "/messages/");
                })
                .verifyComplete();
    }

    // ==================== Error Scenarios ====================

    @Test
    void send_unauthorized_throwsIllegalStateExceptionWith401() throws Exception {
        mockServer.enqueueUnauthorized();

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FCM 401"))
                .verify();
    }

    @Test
    void send_badRequest_throwsIllegalStateExceptionWith400() throws Exception {
        mockServer.enqueueBadRequest("INVALID_ARGUMENT");

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FCM 400"))
                .verify();
    }

    @Test
    void send_notFound_throwsIllegalStateExceptionWith404() throws Exception {
        mockServer.enqueueNotFound();

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FCM 404"))
                .verify();
    }

    @Test
    void send_serverError_throwsIllegalStateExceptionWith500() throws Exception {
        mockServer.enqueueServerError();

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("FCM 500"))
                .verify();
    }

    // ==================== Validation Scenarios ====================

    @Test
    void send_emptyRecipients_throwsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("FCM recipients required"))
                .verify();
    }

    @Test
    void send_nullAddress_throwsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("Test");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(null).build()))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("FCM recipients required"))
                .verify();
    }

    @Test
    void send_verifiesBearerAuthorizationHeader() throws Exception {
        mockServer.enqueueSuccess(null);

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        String authHeader = request.getHeader("Authorization");
        assertThat(authHeader).isNotNull();
        assertThat(authHeader).isEqualTo("Bearer " + ACCESS_TOKEN);
    }

    @Test
    void send_verifiesRequestBody() throws Exception {
        mockServer.enqueueSuccess(null);

        UniMessage message = new UniMessage();
        message.setTitle("Hello");
        message.setContent("World");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        mockServer.verifyRequestBody(request, DEVICE_TOKEN_1, "Hello", "World");
    }

    @Test
    void send_verifiesRequestPath() throws Exception {
        mockServer.enqueueSuccess(null);

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.FCM, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNextCount(1)
                .verifyComplete();

        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request.getPath())
                .isEqualTo("/v1/projects/" + PROJECT_ID + "/messages:send");

        List<String> deviceTokensSent = List.of(DEVICE_TOKEN_1);
        assertThat(deviceTokensSent).isNotEmpty();
    }
}
