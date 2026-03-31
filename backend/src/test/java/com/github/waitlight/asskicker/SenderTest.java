package com.github.waitlight.asskicker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniSendReq;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.service.SendRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderTest {

    @Mock
    private MessageTemplateEngine messageTemplateEngine;

    @Mock
    private ChannelManager channelManager;

    @Mock
    private SendRecordService sendRecordService;

    @Test
    void send_messageDeliveredSuccessfully() {
        Sender sender = new Sender(messageTemplateEngine, channelManager, sendRecordService);
        TestChannel channel = new TestChannel(ChannelType.EMAIL, "test-email", "send-ok");

        UniMessage req = new UniMessage();
        req.setTemplateCode("tpl-code");
        req.setLanguage(Language.ZH_CN);
        req.setTemplateParams(Map.of("name", "lord"));

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .channelProviderKey("provider-key")
                .recipients(Set.of("first@example.com", "second@example.com"))
                .build();

        UniMessage message = new UniMessage();
        message.setTitle("title");
        message.setContent("rendered-content");

        when(messageTemplateEngine.fill(req)).thenReturn(Mono.just(message));
        when(channelManager.chose(ChannelType.EMAIL, "provider-key")).thenReturn(Mono.just(channel));

        StepVerifier.create(sender.send(req, address))
                .assertNext(result -> assertThat(result).isEqualTo("send-ok"))
                .verifyComplete();

        assertThat(channel.lastMessage).isSameAs(message);
        assertThat(channel.lastAddress).isSameAs(address);
        verify(sendRecordService).writeRecord(any());
    }

    @Test
    void send_completesEmpty_whenChannelUnavailable() {
        Sender sender = new Sender(messageTemplateEngine, channelManager, sendRecordService);

        UniMessage req = new UniMessage();
        req.setTemplateCode("tpl-code");
        req.setLanguage(Language.EN);

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.SMS)
                .channelProviderKey("sms-provider")
                .build();

        UniMessage message = new UniMessage();
        message.setContent("hello");

        when(messageTemplateEngine.fill(req)).thenReturn(Mono.just(message));
        when(channelManager.chose(ChannelType.SMS, "sms-provider")).thenReturn(Mono.empty());

        StepVerifier.create(sender.send(req, address)).verifyComplete();
    }

    @Test
    void send_taskRequestDeliveredSuccessfully() {
        Sender sender = new Sender(messageTemplateEngine, channelManager, sendRecordService);
        TestChannel channel = new TestChannel(ChannelType.IM, "test-im", "mq-ok");

        UniMessage req = new UniMessage();
        req.setTemplateCode("tpl-code");
        req.setLanguage(Language.ZH_CN);
        req.setTemplateParams(Map.of("name", "north"));

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.IM)
                .channelProviderKey("chat-target")
                .recipients(Set.of("chat-id-1"))
                .build();

        UniMessage message = new UniMessage();
        message.setTitle("title");
        message.setContent("rendered-content");

        UniSendReq task = UniSendReq.builder()
                .message(req)
                .address(address)
                .taskId("task-001")
                .submittedAt(123456789L)
                .build();

        when(messageTemplateEngine.fill(req)).thenReturn(Mono.just(message));
        when(channelManager.chose(ChannelType.IM, "chat-target")).thenReturn(Mono.just(channel));

        StepVerifier.create(sender.send(task))
                .assertNext(result -> assertThat(result).isEqualTo("mq-ok"))
                .verifyComplete();

        assertThat(channel.lastMessage).isSameAs(message);
        assertThat(channel.lastAddress).isSameAs(address);
        verify(sendRecordService).writeRecord(argThat(record ->
                "task-001".equals(record.getTaskId()) && Long.valueOf(123456789L).equals(record.getSubmittedAt())));
    }

    private static final class TestChannel extends Channel {

        private final String result;
        private UniMessage lastMessage;
        private UniAddress lastAddress;

        private TestChannel(ChannelType channelType, String code, String result) {
            super(buildEntity(channelType, code), WebClient.builder().build(), new ObjectMapper());
            this.result = result;
        }

        @Override
        protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
            this.lastMessage = uniMessage;
            this.lastAddress = uniAddress;
            return Mono.just(result);
        }

        private static ChannelProviderEntity buildEntity(ChannelType channelType, String code) {
            ChannelProviderEntity entity = new ChannelProviderEntity();
            entity.setId(code + "-id");
            entity.setCode(code);
            entity.setChannelType(channelType);
            entity.setEnabled(true);
            return entity;
        }
    }
}
