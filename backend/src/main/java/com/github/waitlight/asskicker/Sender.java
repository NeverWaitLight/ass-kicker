package com.github.waitlight.asskicker;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
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

    public Mono<String> send(UniTask task) {
        if (task == null || task.getMessage() == null || task.getAddress() == null) {
            return Mono.empty();
        }
        return Mono.just(new SendContext(task))
                .flatMap(this::fillMessage)
                .flatMap(this::choseChannel)
                .flatMap(this::sendByChannel)
                .map(this::processSendRecord)
                .map(SendContext::getSendResult);
    }

    private Mono<SendContext> fillMessage(SendContext context) {
        return messageTemplateEngine.fill(context.getTask().getMessage())
                .map(context::withUniMessage);
    }

    private Mono<SendContext> choseChannel(SendContext context) {
        UniAddress uniAddress = context.getTask().getAddress();
        return channelManager.chose(uniAddress.getChannelType(), uniAddress.getChannelProviderKey())
                .map(context::withChannel);
    }

    private SendContext processSendRecord(SendContext context) {
        UniTask task = context.getTask();
        UniMessage message = task.getMessage();
        UniAddress uniAddress = task.getAddress();

        SendRecordEntity sendRecordEntity = new SendRecordEntity();
        sendRecordEntity.setTaskId(task.getTaskId());
        sendRecordEntity.setTemplateCode(message.getTemplateCode());
        sendRecordEntity.setLanguageCode(message.getLanguage().getCode());
        sendRecordEntity.setParams(message.getTemplateParams());
        sendRecordEntity.setChannelId(context.getChannel().getId());
        sendRecordEntity.setRecipient(resolveRecipient(uniAddress));
        sendRecordEntity.setSubmittedAt(resolveSubmittedAt(task));
        sendRecordEntity.setRenderedContent(context.getUniMessage().getContent());
        sendRecordEntity.setChannelType(context.getChannel().getChannelType());
        sendRecordEntity.setChannelName(context.getChannel().getCode());
        sendRecordEntity.setStatus(SendRecordStatus.SUCCESS);
        sendRecordEntity.setSentAt(System.currentTimeMillis());
        sendRecordService.writeRecord(sendRecordEntity);
        return context;
    }

    private Mono<SendContext> sendByChannel(SendContext context) {
        UniTask t = context.getTask();
        UniTask sendTask = UniTask.builder()
                .message(context.getUniMessage())
                .address(t.getAddress())
                .taskId(t.getTaskId())
                .submittedAt(t.getSubmittedAt())
                .build();
        return context.getChannel()
                .send(sendTask)
                .map(context::withSendResult);
    }

    private String resolveRecipient(UniAddress uniAddress) {
        if (uniAddress.getRecipients() == null || uniAddress.getRecipients().isEmpty()) {
            return null;
        }
        return uniAddress.getRecipients().iterator().next();
    }

    private long resolveSubmittedAt(UniTask task) {
        return task.getSubmittedAt() != null ? task.getSubmittedAt() : System.currentTimeMillis();
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static final class SendContext {

        private final UniTask task;
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
