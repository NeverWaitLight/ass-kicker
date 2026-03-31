package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniSendMessageReq;
import com.github.waitlight.asskicker.model.ChannelProviderEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SenderTest {

    @Mock
    private MessageTemplateEngine messageTemplateEngine;

    @Mock
    private ChannelManager channelManager;

    @Test
    void send_runsPipelineInOrder_andReturnsChannelResult() {
        Sender sender = new Sender(messageTemplateEngine, channelManager);
        TestChannel channel = new TestChannel(ChannelType.EMAIL, "test-email", "send-ok");

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("tpl-code");
        req.setLanguage(Language.ZH_CN);
        req.setTemplateParams(Map.of("name", "lord"));

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .channelProviderKey("provider-key")
                .recipients(new LinkedHashSet<>(java.util.List.of("first@example.com", "second@example.com")))
                .build();

        UniMessage message = new UniMessage();
        message.setTitle("title");
        message.setContent("rendered-content");

        when(messageTemplateEngine.fill(req)).thenReturn(Mono.just(message));
        when(channelManager.selectChannel(ChannelType.EMAIL, "provider-key")).thenReturn(Mono.just(channel));

        StepVerifier.create(sender.send(req, address))
                .assertNext(result -> assertThat(result).isEqualTo("send-ok"))
                .verifyComplete();

        assertThat(channel.lastMessage).isSameAs(message);
        assertThat(channel.lastAddress).isSameAs(address);

        InOrder inOrder = inOrder(messageTemplateEngine, channelManager);
        inOrder.verify(messageTemplateEngine).fill(req);
        inOrder.verify(channelManager).selectChannel(ChannelType.EMAIL, "provider-key");
        verifyNoMoreInteractions(messageTemplateEngine, channelManager);
    }

    @Test
    void send_whenNoChannelSelected_completesEmpty() {
        Sender sender = new Sender(messageTemplateEngine, channelManager);

        UniSendMessageReq req = new UniSendMessageReq();
        req.setTemplateCode("tpl-code");
        req.setLanguage(Language.EN);

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.SMS)
                .channelProviderKey("sms-provider")
                .build();

        UniMessage message = new UniMessage();
        message.setContent("hello");

        when(messageTemplateEngine.fill(req)).thenReturn(Mono.just(message));
        when(channelManager.selectChannel(ChannelType.SMS, "sms-provider")).thenReturn(Mono.empty());

        StepVerifier.create(sender.send(req, address)).verifyComplete();

        InOrder inOrder = inOrder(messageTemplateEngine, channelManager);
        inOrder.verify(messageTemplateEngine).fill(req);
        inOrder.verify(channelManager).selectChannel(ChannelType.SMS, "sms-provider");
        verifyNoMoreInteractions(messageTemplateEngine, channelManager);
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
