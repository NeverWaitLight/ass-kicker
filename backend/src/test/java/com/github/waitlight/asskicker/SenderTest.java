package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        @Test
        void send_smsUsesDifferentChannelsForDifferentCountryCodes() throws Exception {
                try (MockWebServer awsServer = new MockWebServer();
                                MockWebServer aliyunServer = new MockWebServer()) {
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

                        UniTask usTask = UniTask.builder()
                                        .message(template)
                                        .address(UniAddress.ofSms("+14155550123"))
                                        .build();
                        UniTask cnTask = UniTask.builder()
                                        .message(template)
                                        .address(UniAddress.ofSms("+8613800138000"))
                                        .build();

                        StepVerifier.create(sender.send(usTask))
                                        .expectNext("AWS_SMS ok 1 recipient(s)")
                                        .verifyComplete();
                        StepVerifier.create(sender.send(cnTask))
                                        .expectNext("ALIYUN_SMS ok 1 recipient(s)")
                                        .verifyComplete();

                        assertThat(awsServer.takeRequest(5, TimeUnit.SECONDS)).isNotNull();
                        assertThat(aliyunServer.takeRequest(5, TimeUnit.SECONDS)).isNotNull();
                }
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
}
