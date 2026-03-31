package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for ApnsChannelHandler using MockWebServer.
 *
 * <p>
 * Tests cover:
 * - Successful push notifications (single and multiple devices)
 * - Error scenarios (400, 401, 410, 500)
 * - Request validation (headers, payload structure)
 */
class ApnsChannelTest {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        private static final String DEVICE_TOKEN_1 = "aabbccddeeff0011223344556677889900aabbccddeeff00112233445566778899";
        private static final String DEVICE_TOKEN_2 = "1122334455667788990011223344556677889900aabbccddeeff001122334455";

        private ApnsMockServer mockServer;
        private ApnsChannel channel;

        /**
         * Creates a ChannelProviderEntity configured for testing.
         *
         * @param mockServerUrl The base URL of the mock server
         */
        private static ChannelProviderEntity createProvider(String mockServerUrl) throws Exception {
                // Use a valid EC private key for JWT signing (test key, not for production)
                String testPrivateKey = """
                                -----BEGIN PRIVATE KEY-----
                                MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgEBNZQdW2XALI6odi
                                sffzbONZ5+i8V1xxzKs88K2KPhShRANCAARfjzU68VEgfLL0eZ38qjls03GvRFwJ
                                NQLOBe4rsFB6lqOYiNME6oCVt4o5Ju46ca2RWappiw8v21uMLZPTLjx5
                                -----END PRIVATE KEY-----
                                """;

                String providerJson = String.format("""
                                {
                                  "name": "APNs Mock Test",
                                  "code": "apns-mock-test",
                                  "channelType": "PUSH",
                                  "providerType": "APNS",
                                  "description": "ApnsChannelHandler test with MockWebServer",
                                  "enabled": true,
                                  "properties": {
                                    "url": "%s",
                                    "bundleIdTopic": "com.example.app",
                                    "teamId": "TEST_TEAM_ID",
                                    "keyId": "TEST_KEY_ID",
                                    "privateKeyPem": "%s"
                                  }
                                }
                                """, mockServerUrl, testPrivateKey.replace("\n", "\\n"));

                return MAPPER.readValue(providerJson, ChannelProviderEntity.class);
        }

        @BeforeEach
        void setUp() throws Exception {
                mockServer = new ApnsMockServer();
                mockServer.start();

                // Create provider configuration using mock server URL
                ChannelProviderEntity provider = createProvider(mockServer.getBaseUrl());
                channel = new ApnsChannel(provider, WebClient.create(), ChannelTestObjectMappers.channelObjectMapper());
        }

        // ==================== Success Scenarios ====================

        @AfterEach
        void tearDown() throws Exception {
                if (mockServer != null) {
                        mockServer.shutdown();
                }
        }

        @Test
        void send_singleDevice_returnsSuccessWithApnsId() throws Exception {
                String apnsId = UUID.randomUUID().toString();
                mockServer.enqueueSuccess(apnsId);

                UniMessage message = new UniMessage();
                message.setTitle("Test Title");
                message.setContent("Test Content");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectNext("APNs ok 1 device(s) apns-id=" + apnsId)
                                .verifyComplete();

                // Verify request
                RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
                assertThat(request).isNotNull();
                assertThat(request.getPath()).isEqualTo("/3/device/" + DEVICE_TOKEN_1);
                mockServer.verifyRequestHeaders(request);
                mockServer.verifyRequestBody(request, "Test Title", "Test Content");
        }

        @Test
        void send_multipleDevices_returnsSuccessWithMultipleApnsIds() throws Exception {
                String apnsId1 = UUID.randomUUID().toString();
                String apnsId2 = UUID.randomUUID().toString();
                mockServer.enqueueSuccess(apnsId1);
                mockServer.enqueueSuccess(apnsId2);

                UniMessage message = new UniMessage();
                message.setTitle("Multi Device Test");
                message.setContent("Testing multiple devices");
                UniAddress address = UniAddress.ofPush(
                                ChannelProviderType.APNS,
                                DEVICE_TOKEN_1, DEVICE_TOKEN_2);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectNext("APNs ok 2 device(s) apns-id=" + apnsId1 + "," + apnsId2)
                                .verifyComplete();

                // Verify both requests (order may vary)
                assertThat(mockServer.getRequestCount()).isEqualTo(2);

                RecordedRequest request1 = mockServer.takeRequest(5, TimeUnit.SECONDS);
                RecordedRequest request2 = mockServer.takeRequest(5, TimeUnit.SECONDS);

                // Verify both device tokens were sent (regardless of order)
                Assertions.assertNotNull(request2.getPath());
                Assertions.assertNotNull(request1.getPath());
                assertThat(List.of(request1.getPath(), request2.getPath()))
                                .containsExactlyInAnyOrder(
                                                "/3/device/" + DEVICE_TOKEN_1,
                                                "/3/device/" + DEVICE_TOKEN_2);

                mockServer.verifyRequestHeaders(request1);
                mockServer.verifyRequestHeaders(request2);
        }

        @Test
        void send_withoutTitle_sendsPayloadWithBodyOnly() throws Exception {
                String apnsId = UUID.randomUUID().toString();
                mockServer.enqueueSuccess(apnsId);

                UniMessage message = new UniMessage();
                message.setContent("Content without title");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectNext("APNs ok 1 device(s) apns-id=" + apnsId)
                                .verifyComplete();

                RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
                mockServer.verifyRequestBody(request, null, "Content without title");
        }

        // ==================== Error Scenarios ====================

        @Test
        void send_autoGeneratesApnsId_whenNotConfigured() throws Exception {
                mockServer.enqueueSuccess(null); // null will auto-generate apns-id

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .assertNext(result -> {
                                        assertThat(result).startsWith("APNs ok 1 device(s) apns-id=");
                                        // Verify UUID format
                                        String apnsId = result.substring("APNs ok 1 device(s) apns-id=".length());
                                        assertThat(apnsId).matches(
                                                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
                                })
                                .verifyComplete();
        }

        @Test
        void send_badDeviceToken_throwsIllegalStateExceptionWith410() throws Exception {
                mockServer.enqueueGoneDeviceToken();

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, "invalid-token");

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectErrorMatches(e -> e instanceof IllegalStateException
                                                && e.getMessage().contains("APNs 410")
                                                && e.getMessage().contains("BadDeviceToken"))
                                .verify();
        }

        @Test
        void send_authenticationFailure_throwsIllegalStateExceptionWith401() throws Exception {
                mockServer.enqueueUnauthorized();

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectErrorMatches(e -> e instanceof IllegalStateException
                                                && e.getMessage().contains("APNs 401")
                                                && e.getMessage().contains("InvalidProviderToken"))
                                .verify();
        }

        @Test
        void send_badRequest_throwsIllegalStateExceptionWith400() throws Exception {
                mockServer.enqueueBadRequest("BadMessageId");

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectErrorMatches(e -> e instanceof IllegalStateException
                                                && e.getMessage().contains("APNs 400")
                                                && e.getMessage().contains("BadMessageId"))
                                .verify();
        }

        // ==================== Validation Scenarios ====================

        @Test
        void send_serverError_throwsIllegalStateExceptionWith500() throws Exception {
                mockServer.enqueueServerError();

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectErrorMatches(e -> e instanceof IllegalStateException
                                                && e.getMessage().contains("APNs 500")
                                                && e.getMessage().contains("InternalServerError"))
                                .verify();
        }

        @Test
        void send_emptyRecipients_throwsIllegalArgumentException() {
                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS); // No device tokens

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                                                && e.getMessage().contains("APNs recipients required"))
                                .verify();
        }

        @Test
        void send_nullAddress_throwsIllegalArgumentException() {
                UniMessage message = new UniMessage();
                message.setContent("Test");

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(null).build()))
                                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                                                && e.getMessage().contains("APNs recipients required"))
                                .verify();
        }

        @Test
        void send_verifiesJwtAuthorizationHeader() throws Exception {
                mockServer.enqueueSuccess(null);

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectNextCount(1)
                                .verifyComplete();

                RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
                String authHeader = request.getHeader("Authorization");
                assertThat(authHeader).isNotNull();
                assertThat(authHeader).startsWith("bearer ");

                // Verify JWT format (3 parts separated by dots)
                String jwt = authHeader.substring("bearer ".length());
                assertThat(jwt.split("\\.")).hasSize(3);
        }

        // ==================== Helper Methods ====================

        @Test
        void send_verifiesApnsHeaders() throws Exception {
                mockServer.enqueueSuccess(null);

                UniMessage message = new UniMessage();
                message.setContent("Test");
                UniAddress address = UniAddress.ofPush(ChannelProviderType.APNS, DEVICE_TOKEN_1);

                StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                                .expectNextCount(1)
                                .verifyComplete();

                RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
                assertThat(request.getHeader("apns-topic")).isEqualTo("com.example.app");
                assertThat(request.getHeader("apns-push-type")).isEqualTo("alert");
                assertThat(request.getHeader("apns-priority")).isEqualTo("10");
                assertThat(request.getHeader("Content-Type")).contains("application/json");
        }
}
