package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.AliyunSmsChannel;
import com.github.waitlight.asskicker.channel.AwsSnsSmsChannel;
import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelProviderType;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.ChannelProviderService;
import com.github.waitlight.asskicker.service.SendRecordService;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SenderTest {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Mock
        private MessageTemplateEngine messageTemplateEngine;

        @Mock
        private SendRecordService sendRecordService;

        @Mock
        private ChannelProviderService channelProviderService;

        @Mock
        private ChannelFactory channelFactory;

        private MockWebServer awsServer;
        private MockWebServer aliyunServer;

        @AfterEach
        void tearDown() throws Exception {
                if (awsServer != null) {
                        awsServer.shutdown();
                        awsServer = null;
                }
                if (aliyunServer != null) {
                        aliyunServer.shutdown();
                        aliyunServer = null;
                }
        }

        @Test
        void send_batchSmsTaskRoutesRecipientsToDifferentChannelInstancesByCountryCode() throws Exception {
                awsServer = new MockWebServer();
                aliyunServer = new MockWebServer();
                awsServer.start();
                aliyunServer.start();

                awsServer.enqueue(new MockResponse().setResponseCode(200)
                                .setHeader("Content-Type", "text/xml")
                                .setBody("""
                                                <PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
                                                        <PublishResult>
                                                                <MessageId>aws-msg-1</MessageId>
                                                        </PublishResult>
                                                </PublishResponse>
                                                """));
                awsServer.enqueue(new MockResponse().setResponseCode(200)
                                .setHeader("Content-Type", "text/xml")
                                .setBody("""
                                                <PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
                                                        <PublishResult>
                                                                <MessageId>aws-msg-2</MessageId>
                                                        </PublishResult>
                                                </PublishResponse>
                                                """));
                aliyunServer.enqueue(new MockResponse().setResponseCode(200)
                                .setHeader("Content-Type", "application/json;charset=UTF-8")
                                .setBody("{\"Code\":\"OK\",\"Message\":\"OK\",\"RequestId\":\"aliyun-msg-1\"}"));

                ChannelProviderEntity usProvider = buildAwsProvider(awsServer.url("/").toString());
                ChannelProviderEntity cnProvider = buildAliyunProvider(aliyunServer.url("/").toString());

                AwsSnsSmsChannel usChannel = new AwsSnsSmsChannel(usProvider, WebClient.create(),
                                OBJECT_MAPPER);
                AliyunSmsChannel cnChannel = new AliyunSmsChannel(cnProvider, WebClient.create(),
                                OBJECT_MAPPER);

                when(channelProviderService.findEnabled()).thenReturn(Flux.just(usProvider, cnProvider));
                when(channelFactory.create(usProvider)).thenReturn(usChannel);
                when(channelFactory.create(cnProvider)).thenReturn(cnChannel);

                ChannelManager channelManager = new ChannelManager(channelProviderService, channelFactory);
                channelManager.refresh();

                Sender sender = new Sender(messageTemplateEngine, channelManager, sendRecordService);
                UniMessage template = buildTemplate();
                UniMessage renderedMessage = new UniMessage();
                renderedMessage.setContent("rendered-content");
                when(messageTemplateEngine.fill(template)).thenReturn(Mono.just(renderedMessage));

                String usRecipient1 = "+14155550123";
                String cnRecipient = "+8613800138000";
                String usRecipient2 = "+12065550100";
                UniTask batchTask = UniTask.builder()
                                .message(template)
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(new LinkedHashSet<>(List.of(
                                                                usRecipient1,
                                                                cnRecipient,
                                                                usRecipient2)))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(batchTask))
                                .consumeNextWith(result -> assertThat(result.split(","))
                                                .containsExactly(
                                                                "AWS_SMS ok 1 recipient(s)",
                                                                "ALIYUN_SMS ok 1 recipient(s)",
                                                                "AWS_SMS ok 1 recipient(s)"))
                                .verifyComplete();

                assertThat(List.of(
                                extractFormParam(takeRequest(awsServer), "PhoneNumber"),
                                extractFormParam(takeRequest(awsServer), "PhoneNumber")))
                                .containsExactly(usRecipient1, usRecipient2);
                assertThat(extractFormParam(takeRequest(aliyunServer), "PhoneNumbers"))
                                .isEqualTo(cnRecipient);
                assertThat(awsServer.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();
                assertThat(aliyunServer.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();

                ArgumentCaptor<com.github.waitlight.asskicker.model.SendRecordEntity> recordCaptor = ArgumentCaptor
                                .forClass(com.github.waitlight.asskicker.model.SendRecordEntity.class);
                verify(sendRecordService, times(3)).writeRecord(recordCaptor.capture());
                assertThat(recordCaptor.getAllValues())
                                .extracting(
                                                com.github.waitlight.asskicker.model.SendRecordEntity::getRecipient,
                                                com.github.waitlight.asskicker.model.SendRecordEntity::getChannelId,
                                                com.github.waitlight.asskicker.model.SendRecordEntity::getChannelName)
                                .containsExactlyInAnyOrder(
                                                tuple(usRecipient1, "a-us-sms-id", "a-us-sms"),
                                                tuple(cnRecipient, "z-cn-sms-id", "z-cn-sms"),
                                                tuple(usRecipient2, "a-us-sms-id", "a-us-sms"));
        }

        private static UniMessage buildTemplate() {
                UniMessage message = new UniMessage();
                message.setTemplateCode("sms-template");
                message.setLanguage(Language.ZH_CN);
                return message;
        }

        private static ChannelProviderEntity buildAwsProvider(String endpoint) {
                ChannelProviderEntity entity = new ChannelProviderEntity();
                entity.setId("a-us-sms-id");
                entity.setCode("a-us-sms");
                entity.setChannelType(ChannelType.SMS);
                entity.setProviderType(ChannelProviderType.AWS_SMS);
                entity.setPriorityAddressRegex("^\\+1\\d+$");
                entity.setEnabled(true);
                entity.setProperties(Map.of(
                                "accessKeyId", "AKIAIOSFODNN7EXAMPLE",
                                "secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                                "region", "us-east-1",
                                "endpoint", endpoint));
                return entity;
        }

        private static ChannelProviderEntity buildAliyunProvider(String endpoint) {
                ChannelProviderEntity entity = new ChannelProviderEntity();
                entity.setId("z-cn-sms-id");
                entity.setCode("z-cn-sms");
                entity.setChannelType(ChannelType.SMS);
                entity.setProviderType(ChannelProviderType.ALIYUN_SMS);
                entity.setPriorityAddressRegex("^\\+86\\d+$");
                entity.setEnabled(true);
                entity.setProperties(Map.of(
                                "accessKeyId", "test-ak",
                                "accessKeySecret", "test-sk",
                                "signName", "test-sign",
                                "templateCode", "SMS_TPL",
                                "endpoint", endpoint));
                return entity;
        }

        private static RecordedRequest takeRequest(MockWebServer server) throws InterruptedException {
                RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
                assertThat(request).isNotNull();
                assertThat(request.getMethod()).isEqualTo("POST");
                return request;
        }

        private static String extractFormParam(RecordedRequest request, String key) {
                String body = request.getBody().readString(StandardCharsets.UTF_8);
                return body.lines()
                                .flatMap(line -> List.of(line.split("&")).stream())
                                .map(pair -> pair.split("=", 2))
                                .filter(parts -> parts.length == 2)
                                .filter(parts -> key.equals(urlDecode(parts[0])))
                                .map(parts -> urlDecode(parts[1]))
                                .findFirst()
                                .orElseThrow(() -> new AssertionError(
                                                "Missing form param: " + key + " in body " + body));
        }

        private static String urlDecode(String value) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
}
