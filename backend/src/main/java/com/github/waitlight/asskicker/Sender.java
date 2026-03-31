package com.github.waitlight.asskicker;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniSendReq;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.SendRecordEntity;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.service.SendRecordService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final MessageTemplateEngine messageTemplateEngine;
    private final ChannelManager channelManager;
    private final SendRecordService sendRecordService;

    public Mono<String> send(UniSendReq req) {
        if (req == null || req.getMessage() == null || req.getAddress() == null) {
            return Mono.empty();
        }
        return Mono.just(new SendContext(req))
                .flatMap(this::fillMessage)
                .flatMap(this::choseChannel)
                .flatMap(this::sendByChannel)
                .map(this::processSendRecord)
                .map(SendContext::getSendResult);
    }

    private Mono<SendContext> fillMessage(SendContext context) {
        return messageTemplateEngine.fill(context.getRequest().getMessage())
                .map(context::withUniMessage);
    }

    private Mono<SendContext> choseChannel(SendContext context) {
        UniAddress uniAddress = context.getRequest().getAddress();
        return channelManager.chose(uniAddress.getChannelType(), uniAddress.getChannelProviderKey())
                .map(context::withChannel);
    }

    private SendContext processSendRecord(SendContext context) {
        UniSendReq request = context.getRequest();
        UniMessage req = request.getMessage();
        UniAddress uniAddress = request.getAddress();

        SendRecordEntity sendRecordEntity = new SendRecordEntity();
        sendRecordEntity.setTaskId(request.getTaskId());
        sendRecordEntity.setTemplateCode(req.getTemplateCode());
        sendRecordEntity.setLanguageCode(req.getLanguage().getCode());
        sendRecordEntity.setParams(req.getTemplateParams());
        sendRecordEntity.setChannelId(context.getChannel().getId());
        sendRecordEntity.setRecipient(resolveRecipient(uniAddress));
        sendRecordEntity.setSubmittedAt(resolveSubmittedAt(request));
        sendRecordEntity.setRenderedContent(context.getUniMessage().getContent());
        sendRecordEntity.setChannelType(context.getChannel().getChannelType());
        sendRecordEntity.setChannelName(context.getChannel().getCode());
        sendRecordEntity.setStatus(SendRecordStatus.SUCCESS);
        sendRecordEntity.setSentAt(System.currentTimeMillis());
        sendRecordService.writeRecord(sendRecordEntity);
        return context;
    }

    private Mono<SendContext> sendByChannel(SendContext context) {
        return context.getChannel()
                .send(context.getUniMessage(), context.getRequest().getAddress())
                .map(context::withSendResult);
    }

    private String resolveRecipient(UniAddress uniAddress) {
        if (uniAddress.getRecipients() == null || uniAddress.getRecipients().isEmpty()) {
            return null;
        }
        return uniAddress.getRecipients().iterator().next();
    }

    private long resolveSubmittedAt(UniSendReq request) {
        return request.getSubmittedAt() != null ? request.getSubmittedAt() : System.currentTimeMillis();
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static final class SendContext {

        private final UniSendReq request;
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
