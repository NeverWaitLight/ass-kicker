package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.impl.ApnsPushChannel;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test suite for ApnsPushChannel using a mocked Pushy ApnsClient.
 *
 * <p>Pushy owns JWT signing, HTTP/2 framing, and APNs header construction internally;
 * tests therefore mock {@link ApnsClient} and assert against the
 * {@link SimpleApnsPushNotification} the channel hands off to Pushy.
 */
@ExtendWith(MockitoExtension.class)
class ApnsPushChannelTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String DEVICE_TOKEN_1 = "aabbccddeeff0011223344556677889900aabbccddeeff00112233445566778899";
    private static final String DEVICE_TOKEN_2 = "1122334455667788990011223344556677889900aabbccddeeff001122334455";
    private static final String TOPIC = "com.example.app";

    @Mock
    private ApnsClient apnsClient;

    private ApnsPushChannel channel;

    private static ChannelEntity createProvider() throws Exception {
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
                  "type": "PUSH",
                  "provider": "APPLE",
                  "providerType": "APNS",
                  "description": "ApnsPushChannel test with mocked ApnsClient",
                  "enabled": true,
                  "properties": {
                    "bundleIdTopic": "%s",
                    "teamId": "TEST_TEAM_ID",
                    "keyId": "TEST_KEY_ID",
                    "privateKeyPem": "%s"
                  }
                }
                """, TOPIC, testPrivateKey.replace("\n", "\\n"));
        return MAPPER.readValue(providerJson, ChannelEntity.class);
    }

    private static PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
            completed(SimpleApnsPushNotification dummyNotification,
                      PushNotificationResponse<SimpleApnsPushNotification> response) {
        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> f =
                new PushNotificationFuture<>(dummyNotification);
        f.complete(response);
        return f;
    }

    @SuppressWarnings("unchecked")
    private PushNotificationResponse<SimpleApnsPushNotification> okResponse(UUID apnsId) {
        PushNotificationResponse<SimpleApnsPushNotification> resp = org.mockito.Mockito.mock(PushNotificationResponse.class);
        when(resp.isAccepted()).thenReturn(true);
        when(resp.getApnsId()).thenReturn(apnsId);
        return resp;
    }

    @SuppressWarnings("unchecked")
    private PushNotificationResponse<SimpleApnsPushNotification> rejectedResponse(int statusCode, String reason) {
        PushNotificationResponse<SimpleApnsPushNotification> resp = org.mockito.Mockito.mock(PushNotificationResponse.class);
        when(resp.isAccepted()).thenReturn(false);
        when(resp.getStatusCode()).thenReturn(statusCode);
        when(resp.getRejectionReason()).thenReturn(Optional.of(reason));
        return resp;
    }

    @BeforeEach
    void setUp() throws Exception {
        ChannelEntity provider = createProvider();
        channel = ApnsPushChannel.forTesting(provider, WebClient.create(),
                ChannelTestObjectMappers.channelObjectMapper(), apnsClient);
    }

    // ==================== Success Scenarios ====================

    @Test
    void send_singleDevice_returnsSuccessWithApnsId() {
        UUID apnsId = UUID.randomUUID();
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = okResponse(apnsId);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setTitle("Test Title");
        message.setContent("Test Content");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("APNs ok 1 device(s) apns-id=" + apnsId)
                .verifyComplete();

        ArgumentCaptor<SimpleApnsPushNotification> captor = ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient, times(1)).sendNotification(captor.capture());
        SimpleApnsPushNotification sent = captor.getValue();
        assertThat(sent.getToken()).isEqualTo(DEVICE_TOKEN_1);
        assertThat(sent.getTopic()).isEqualTo(TOPIC);
        assertThat(sent.getPriority()).isEqualTo(DeliveryPriority.IMMEDIATE);
        assertThat(sent.getPushType()).isEqualTo(PushType.ALERT);
        assertThat(sent.getPayload()).contains("\"title\":\"Test Title\"");
        assertThat(sent.getPayload()).contains("\"body\":\"Test Content\"");
    }

    @Test
    void send_multipleDevices_returnsSuccessWithMultipleApnsIds() {
        UUID apnsId1 = UUID.randomUUID();
        UUID apnsId2 = UUID.randomUUID();
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp1 = okResponse(apnsId1);
        PushNotificationResponse<SimpleApnsPushNotification> resp2 = okResponse(apnsId2);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp1))
                .thenReturn(completed(dummy, resp2));

        UniMessage message = new UniMessage();
        message.setTitle("Multi Device Test");
        message.setContent("Testing multiple devices");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1, DEVICE_TOKEN_2);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("APNs ok 2 device(s) apns-id=" + apnsId1 + "," + apnsId2)
                .verifyComplete();

        ArgumentCaptor<SimpleApnsPushNotification> captor = ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient, times(2)).sendNotification(captor.capture());
        List<SimpleApnsPushNotification> sent = captor.getAllValues();
        assertThat(sent).extracting(SimpleApnsPushNotification::getToken)
                .containsExactly(DEVICE_TOKEN_1, DEVICE_TOKEN_2);
    }

    @Test
    void send_withoutTitle_sendsPayloadWithBodyOnly() throws Exception {
        UUID apnsId = UUID.randomUUID();
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = okResponse(apnsId);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Content without title");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("APNs ok 1 device(s) apns-id=" + apnsId)
                .verifyComplete();

        ArgumentCaptor<SimpleApnsPushNotification> captor = ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient).sendNotification(captor.capture());
        Map<String, Object> payload = JSON.readValue(captor.getValue().getPayload(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> aps = (Map<String, Object>) payload.get("aps");
        @SuppressWarnings("unchecked")
        Map<String, Object> alert = (Map<String, Object>) aps.get("alert");
        assertThat(alert.containsKey("title")).isFalse();
        assertThat(alert.get("body")).isEqualTo("Content without title");
    }

    @Test
    void send_responseWithoutApnsId_returnsOk() {
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = okResponse(null);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNext("APNs ok 1 device(s) apns-id=ok")
                .verifyComplete();
    }

    // ==================== Error Scenarios ====================

    @Test
    void send_badDeviceToken_throwsIllegalStateExceptionWith410() {
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification("invalid-token", TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = rejectedResponse(410, "BadDeviceToken");
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, "invalid-token");

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("APNs 410")
                        && e.getMessage().contains("BadDeviceToken"))
                .verify();
    }

    @Test
    void send_authenticationFailure_throwsIllegalStateExceptionWith401() {
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = rejectedResponse(401, "InvalidProviderToken");
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("APNs 401")
                        && e.getMessage().contains("InvalidProviderToken"))
                .verify();
    }

    @Test
    void send_badRequest_throwsIllegalStateExceptionWith400() {
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = rejectedResponse(400, "BadMessageId");
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("APNs 400")
                        && e.getMessage().contains("BadMessageId"))
                .verify();
    }

    @Test
    void send_serverError_throwsIllegalStateExceptionWith500() {
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = rejectedResponse(500, "InternalServerError");
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("APNs 500")
                        && e.getMessage().contains("InternalServerError"))
                .verify();
    }

    // ==================== Validation Scenarios ====================

    @Test
    void send_emptyRecipients_throwsIllegalArgumentException() {
        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS);

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
    void send_passesBundleIdTopicToPushy() {
        UUID apnsId = UUID.randomUUID();
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = okResponse(apnsId);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<SimpleApnsPushNotification> captor = ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient).sendNotification(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(TOPIC);
    }

    @Test
    void send_passesAlertPushTypeAndImmediatePriority() {
        UUID apnsId = UUID.randomUUID();
        SimpleApnsPushNotification dummy = new SimpleApnsPushNotification(DEVICE_TOKEN_1, TOPIC, "{}");
        PushNotificationResponse<SimpleApnsPushNotification> resp = okResponse(apnsId);
        when(apnsClient.sendNotification(any(SimpleApnsPushNotification.class)))
                .thenReturn(completed(dummy, resp));

        UniMessage message = new UniMessage();
        message.setContent("Test");
        UniAddress address = UniAddress.ofPush(ProviderType.APNS, DEVICE_TOKEN_1);

        StepVerifier.create(channel.send(UniTask.builder().message(message).address(address).build()))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<SimpleApnsPushNotification> captor = ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient).sendNotification(captor.capture());
        assertThat(captor.getValue().getPushType()).isEqualTo(PushType.ALERT);
        assertThat(captor.getValue().getPriority()).isEqualTo(DeliveryPriority.IMMEDIATE);
    }
}
