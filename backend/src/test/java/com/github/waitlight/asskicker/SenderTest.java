package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.waitlight.asskicker.model.*;
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

import okhttp3.HttpUrl;
import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.service.SendRecordService;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;

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
        private TemplateEngine templateEngine;

        @Mock
        private SendRecordService sendRecordService;

        @Mock
        private ChannelService channelService;

        @Mock
        private ChannelFactory channelFactory;

        @Mock
        private ChannelManager channelManager;

        @Mock
        private SnowflakeIdGenerator snowflakeIdGenerator;

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

                ChannelEntity usProvider = buildAwsProvider(awsServer.url("/").toString());
                ChannelEntity cnProvider = buildAliyunProvider(aliyunServer.url("/").toString());

                AwsSnsSmsChannel usChannel = new AwsSnsSmsChannel(usProvider, WebClient.create(),
                                OBJECT_MAPPER);
                AliyunSmsChannel cnChannel = new AliyunSmsChannel(cnProvider, WebClient.create(),
                                OBJECT_MAPPER);

                when(channelService.findEnabled()).thenReturn(Flux.just(usProvider, cnProvider));
                when(channelFactory.create(usProvider)).thenReturn(usChannel);
                when(channelFactory.create(cnProvider)).thenReturn(cnChannel);

                ChannelManager channelManager = new ChannelManager(channelService, channelFactory);
                channelManager.refresh();

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                UniMessage template = buildTemplate();
                UniMessage renderedMessage = new UniMessage();
                renderedMessage.setContent("rendered-content");
                when(templateEngine.fill(template)).thenReturn(Mono.just(renderedMessage));

                String usRecipient1 = "+14155550123";
                String cnRecipient = "+8613800138000";
                String usRecipient2 = "+12065550100";
                UniTask batchTask = UniTask.builder()
                                .message(template)
                                .taskId("batch-task-1")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(new LinkedHashSet<>(List.of(
                                                                usRecipient1,
                                                                cnRecipient,
                                                                usRecipient2)))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(batchTask))
                                .expectNext("batch-task-1")
                                .verifyComplete();

                ArgumentCaptor<SendRecordEntity> recordCaptor = ArgumentCaptor
                                .forClass(SendRecordEntity.class);
                verify(sendRecordService, timeout(10000).times(3)).writeRecord(recordCaptor.capture());

                assertThat(List.of(
                                extractFormParam(takeRequest(awsServer), "PhoneNumber"),
                                extractFormParam(takeRequest(awsServer), "PhoneNumber")))
                                .containsExactlyInAnyOrder(usRecipient1, usRecipient2);
                assertAliyunPayloadHasRecipient(recordedRequestPayload(takeRequest(aliyunServer)), cnRecipient);
                assertThat(awsServer.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();
                assertThat(aliyunServer.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();
                assertThat(recordCaptor.getAllValues())
                                .extracting(
                                                SendRecordEntity::getRecipient,
                                                SendRecordEntity::getChannelId,
                                                SendRecordEntity::getChannelName)
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

        private static ChannelEntity buildAwsProvider(String endpoint) {
                ChannelEntity entity = new ChannelEntity();
                entity.setId("a-us-sms-id");
                entity.setCode("a-us-sms");
                entity.setChannelType(ChannelType.SMS);
                entity.setProviderType(ProviderType.AWS_SMS);
                entity.setPriorityAddressRegex("^\\+1\\d+$");
                entity.setEnabled(true);
                entity.setProperties(Map.of(
                                "accessKeyId", "AKIAIOSFODNN7EXAMPLE",
                                "secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                                "region", "us-east-1",
                                "endpoint", endpoint));
                return entity;
        }

        private static ChannelEntity buildAliyunProvider(String endpoint) {
                ChannelEntity entity = new ChannelEntity();
                entity.setId("z-cn-sms-id");
                entity.setCode("z-cn-sms");
                entity.setChannelType(ChannelType.SMS);
                entity.setProviderType(ProviderType.ALIYUN_SMS);
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

        /** 阿里云官方 SDK 可能把参数放在 URL 或 JSON 体内，集成测试只要求能观察到收件人号码 */
        private static String recordedRequestPayload(RecordedRequest request) {
                HttpUrl url = request.getRequestUrl();
                String body = request.getBody().readString(StandardCharsets.UTF_8);
                return (url != null ? url.toString() : "") + "\n" + body;
        }

        private static void assertAliyunPayloadHasRecipient(String payload, String e164) {
                assertThat(payload.contains(e164) || payload.contains(e164.replace("+", "%2B"))).isTrue();
        }

        private static String urlDecode(String value) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        @Test
        void send_generatesTaskIdWhenMissingOrBlank() {
                UniMessage template = buildTemplate();
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fill(any())).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());
                when(snowflakeIdGenerator.nextIdString()).thenReturn("snowflake-1", "snowflake-2");

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                UniTask missingId = UniTask.builder()
                                .message(template)
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(missingId))
                                .expectNext("snowflake-1")
                                .verifyComplete();
                assertThat(missingId.getTaskId()).isEqualTo("snowflake-1");

                UniTask blankId = UniTask.builder()
                                .message(template)
                                .taskId("   ")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(blankId))
                                .expectNext("snowflake-2")
                                .verifyComplete();
                assertThat(blankId.getTaskId()).isEqualTo("snowflake-2");
                verify(channelManager, timeout(5000).times(2)).chose(any(ChannelType.class), anyString());
        }

        @Test
        void send_preservesExistingTaskIdAndFillsSubmittedAtWhenMissing() {
                UniMessage template = buildTemplate();
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fill(any())).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                UniTask task = UniTask.builder()
                                .message(template)
                                .taskId("fixed-task-id")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("fixed-task-id")
                                .verifyComplete();
                assertThat(task.getTaskId()).isEqualTo("fixed-task-id");
                assertThat(task.getSubmittedAt()).isNotNull();
                verify(channelManager, timeout(5000)).chose(any(ChannelType.class), anyString());
        }

        @Test
        void send_writesFailedRecordWhenTemplateFillReturnsNull() {
                when(templateEngine.fill(any())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-fill-null")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123", "+14155550124"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-fill-null")
                                .verifyComplete();

                ArgumentCaptor<SendRecordEntity> captor = ArgumentCaptor.forClass(SendRecordEntity.class);
                verify(sendRecordService, timeout(5000).times(1)).writeRecord(captor.capture());

                SendRecordEntity record = captor.getValue();
                assertThat(record.getTaskId()).isEqualTo("task-fill-null");
                assertThat(record.getStatus()).isEqualTo(com.github.waitlight.asskicker.model.SendRecordStatus.FAILED);
                assertThat(record.getRecipient()).isNull();
                assertThat(record.getErrorMessage()).isNotBlank();
        }

        @Test
        void send_writesFailedRecordWhenTemplateFillThrows() {
                when(templateEngine.fill(any()))
                                .thenReturn(Mono.error(new RuntimeException("template-engine-error")));

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-fill-throws")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-fill-throws")
                                .verifyComplete();

                ArgumentCaptor<SendRecordEntity> captor = ArgumentCaptor.forClass(SendRecordEntity.class);
                verify(sendRecordService, timeout(5000).times(1)).writeRecord(captor.capture());

                SendRecordEntity record = captor.getValue();
                assertThat(record.getTaskId()).isEqualTo("task-fill-throws");
                assertThat(record.getStatus()).isEqualTo(com.github.waitlight.asskicker.model.SendRecordStatus.FAILED);
                assertThat(record.getRecipient()).isNull();
                assertThat(record.getErrorMessage()).contains("template-engine-error");
        }

        @Test
        void send_writesFailedRecordPerRecipientWhenChannelNotFound() {
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fill(any())).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, sendRecordService,
                                snowflakeIdGenerator);
                String r1 = "+14155550123";
                String r2 = "+14155550124";
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-no-channel")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(new LinkedHashSet<>(List.of(r1, r2)))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-no-channel")
                                .verifyComplete();

                ArgumentCaptor<SendRecordEntity> captor = ArgumentCaptor.forClass(SendRecordEntity.class);
                verify(sendRecordService, timeout(5000).times(2)).writeRecord(captor.capture());

                assertThat(captor.getAllValues())
                                .extracting(SendRecordEntity::getRecipient,
                                                SendRecordEntity::getStatus)
                                .containsExactlyInAnyOrder(
                                                tuple(r1, com.github.waitlight.asskicker.model.SendRecordStatus.FAILED),
                                                tuple(r2, com.github.waitlight.asskicker.model.SendRecordStatus.FAILED));
                assertThat(captor.getAllValues())
                                .allMatch(r -> r.getErrorMessage() != null && !r.getErrorMessage().isBlank());
        }
}
