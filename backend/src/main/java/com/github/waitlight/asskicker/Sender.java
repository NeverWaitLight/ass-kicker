package com.github.waitlight.asskicker;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniMessageReq;
import com.github.waitlight.asskicker.model.SendRecordEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final MessageTemplateEngine messageTemplateEngine;
    private final ChannelManager channelManager;

    public Mono<String> send(UniMessageReq req, UniAddress uniAddress) {
        return Mono.just(new SendContext(req, uniAddress))
                .flatMap(this::fillMessage)
                .flatMap(this::selectChannel)
                .flatMap(this::sendByChannel)
                .map(this::processSendRecord)
                .map(SendContext::getSendResult);
    }

    private Mono<SendContext> fillMessage(SendContext context) {
        return messageTemplateEngine.fill(context.getReq())
                .map(context::withUniMessage);
    }

    private Mono<SendContext> selectChannel(SendContext context) {
        UniAddress uniAddress = context.getUniAddress();
        return channelManager.selectChannel(uniAddress.getChannelType(), uniAddress.getChannelProviderKey())
                .map(context::withChannel);
    }

    private SendContext processSendRecord(SendContext context) {
        UniMessageReq req = context.getReq();
        UniAddress uniAddress = context.getUniAddress();
        SendRecordEntity sendRecordEntity = new SendRecordEntity();
        sendRecordEntity.setTaskId(null);
        sendRecordEntity.setTemplateCode(req.getTemplateCode());
        sendRecordEntity.setLanguageCode(req.getLanguage().getCode());
        sendRecordEntity.setParams(req.getTemplateParams());
        sendRecordEntity.setChannelId(uniAddress.getChannelProviderKey());
        sendRecordEntity.setRecipient(resolveRecipient(uniAddress));
        sendRecordEntity.setSubmittedAt(System.currentTimeMillis());
        sendRecordEntity.setRenderedContent(context.getUniMessage().getContent());
        return context;
    }

    private Mono<SendContext> sendByChannel(SendContext context) {
        return context.getChannel()
                .send(context.getUniMessage(), context.getUniAddress())
                .map(context::withSendResult);
    }

    private String resolveRecipient(UniAddress uniAddress) {
        if (uniAddress.getRecipients() == null || uniAddress.getRecipients().isEmpty()) {
            return null;
        }
        return uniAddress.getRecipients().iterator().next();
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static final class SendContext {

        private final UniMessageReq req;
        private final UniAddress uniAddress;
        private UniMessage uniMessage;
        private Channel channel;
        private String sendResult;

        private SendContext withUniMessage(UniMessage uniMessage) {
            this.uniMessage = uniMessage;
            return this;
        }

        private SendContext withChannel(Channel channel) {
            this.channel = channel;
            return this;
        }

        private SendContext withSendResult(String sendResult) {
            this.sendResult = sendResult;
            return this;
        }
    }
}
