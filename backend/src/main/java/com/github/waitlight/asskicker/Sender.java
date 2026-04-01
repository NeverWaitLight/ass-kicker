package com.github.waitlight.asskicker;

import java.util.Set;
import java.util.stream.Collectors;

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
import reactor.core.publisher.Flux;
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

        Set<String> recipients = task.getAddress().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return Mono.empty();
        }

        return Mono.just(new SendContext(task))
                .flatMap(this::fillMessage)
                .flatMapMany(ctx -> Flux.fromIterable(recipients)
                        .concatMap(recipient -> doSend(ctx, recipient)))
                .map(SendContext::getSendResult)
                .collect(Collectors.joining(","));
    }

    private Mono<SendContext> fillMessage(SendContext context) {
        return messageTemplateEngine.fill(context.getTask().getMessage())
                .map(context::withUniMessage);
    }

    private Mono<SendContext> doSend(SendContext baseCtx, String recipient) {
        UniAddress singleAddr = buildSingleRecipientAddress(baseCtx.getTask().getAddress(), recipient);
        return channelManager.chose(singleAddr.getChannelType(), recipient)
                .map(channel -> baseCtx.fork(recipient, channel, singleAddr))
                .flatMap(this::sendByChannel)
                .map(this::processSendRecord);
    }

    private UniAddress buildSingleRecipientAddress(UniAddress original, String recipient) {
        return UniAddress.builder()
                .channelType(original.getChannelType())
                .channelProviderType(original.getChannelProviderType())
                .channelProviderKey(original.getChannelProviderKey())
                .recipients(Set.of(recipient))
                .build();
    }

    private SendContext processSendRecord(SendContext context) {
        UniTask task = context.getTask();
        UniMessage message = task.getMessage();

        SendRecordEntity sr = new SendRecordEntity();
        sr.setTaskId(task.getTaskId());
        sr.setTemplateCode(message.getTemplateCode());
        sr.setLanguageCode(message.getLanguage().getCode());
        sr.setParams(message.getTemplateParams());
        sr.setChannelId(context.getChannel().getId());
        sr.setRecipient(context.getRecipient());
        sr.setSubmittedAt(resolveSubmittedAt(task));
        sr.setRenderedContent(context.getUniMessage().getContent());
        sr.setChannelType(context.getChannel().getChannelType());
        sr.setChannelName(context.getChannel().getCode());
        sr.setStatus(SendRecordStatus.SUCCESS);
        sr.setSentAt(System.currentTimeMillis());
        sendRecordService.writeRecord(sr);
        return context;
    }

    private Mono<SendContext> sendByChannel(SendContext context) {
        UniTask t = context.getTask();
        UniTask sendTask = UniTask.builder()
                .message(context.getUniMessage())
                .address(context.getSingleAddress())
                .taskId(t.getTaskId())
                .submittedAt(t.getSubmittedAt())
                .build();
        return context.getChannel()
                .send(sendTask)
                .map(context::withSendResult);
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
        private String recipient;
        private UniAddress singleAddress;

        private SendContext withUniMessage(UniMessage uniMessage) {
            this.uniMessage = uniMessage;
            return this;
        }

        private SendContext withSendResult(String sendResult) {
            this.sendResult = sendResult;
            return this;
        }

        private SendContext fork(String recipient, Channel channel, UniAddress singleAddress) {
            SendContext forked = new SendContext(this.task);
            forked.uniMessage = this.uniMessage;
            forked.recipient = recipient;
            forked.channel = channel;
            forked.singleAddress = singleAddress;
            return forked;
        }
    }
}
