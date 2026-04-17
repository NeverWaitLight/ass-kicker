package com.github.waitlight.asskicker;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.RecordEntity;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.service.RecordService;
import com.github.waitlight.asskicker.util.SnowflakeIdGenerator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final TemplateEngine templateEngine;
    private final ChannelManager channelManager;
    private final RecordService recordService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Shutting down sender executor...");
        executor.shutdown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        while (!executor.isTerminated() && System.nanoTime() < deadline) {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        if (!executor.isTerminated()) {
            log.warn("Sender executor did not terminate in time, forcing shutdown");
            executor.shutdownNow();
        }
    }

    public Mono<String> send(UniTask task) {
        if (task == null || task.getMessage() == null || task.getAddress() == null) {
            return Mono.empty();
        }

        Set<String> recipients = task.getAddress().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return Mono.empty();
        }

        normalize(task);

        executor.submit(() -> process(task));

        return Mono.just(task.getTaskId());
    }

    private void normalize(UniTask task) {
        if (task.getTaskId() == null || task.getTaskId().isBlank()) {
            task.setTaskId(snowflakeIdGenerator.nextIdString());
        }
        if (task.getSubmittedAt() == null) {
            task.setSubmittedAt(Instant.now().toEpochMilli());
        }
    }

    private void process(UniTask task) {
        UniMessage filled = null;
        try {
            filled = templateEngine.fill(task.getMessage()).block();
            if (filled == null) {
                log.warn("Template fill returned null for taskId={}", task.getTaskId());
                writeFailedRecord(task, null, null, "Template fill returned null");
                return;
            }
        } catch (Exception e) {
            log.error("Failed to fill message template taskId={}", task.getTaskId(), e);
            writeFailedRecord(task, null, null, "Template fill stage failed: " + e.getMessage());
            return;
        }

        Set<String> recipients = task.getAddress().getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        SendContext baseCtx = new SendContext(task);
        baseCtx.setUniMessage(filled);

        for (String recipient : recipients) {
            executor.submit(() -> doSendForRecipient(baseCtx, recipient));
        }
    }

    private void doSendForRecipient(SendContext baseCtx, String recipient) {
        Channel channel = null;
        try {
            UniAddress singleAddr = buildSingleRecipientAddress(baseCtx.getTask().getAddress(), recipient);
            channel = channelManager.chose(singleAddr.getChannelType(), recipient).block();
            if (channel == null) {
                log.warn("No channel available for recipient={} taskId={}", recipient, baseCtx.getTask().getTaskId());
                writeFailedRecord(baseCtx.getTask(), recipient, null, "No channel available");
                return;
            }

            SendContext ctx = baseCtx.fork(recipient, channel, singleAddr);

            UniTask sendTask = UniTask.builder()
                    .message(ctx.getUniMessage())
                    .address(ctx.getSingleAddress())
                    .taskId(ctx.getTask().getTaskId())
                    .submittedAt(ctx.getTask().getSubmittedAt())
                    .build();

            String result = channel.send(sendTask).block();
            ctx.setSendResult(result);

            processRecord(ctx);
        } catch (Exception e) {
            log.error("Failed to send to recipient={} taskId={}", recipient, baseCtx.getTask().getTaskId(), e);
            writeFailedRecord(baseCtx.getTask(), recipient, channel, e.getMessage());
        }
    }

    private UniAddress buildSingleRecipientAddress(UniAddress original, String recipient) {
        return UniAddress.builder()
                .channelType(original.getChannelType())
                .providerType(original.getProviderType())
                .channelKey(original.getChannelKey())
                .recipients(Set.of(recipient))
                .build();
    }

    private void processRecord(SendContext context) {
        UniTask task = context.getTask();
        UniMessage message = task.getMessage();

        RecordEntity sr = new RecordEntity();
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
        recordService.writeRecord(sr);
    }

    private void writeFailedRecord(UniTask task, String recipient, Channel channel, String errorMessage) {
        UniMessage message = task.getMessage();

        RecordEntity sr = new RecordEntity();
        sr.setTaskId(task.getTaskId());
        if (message != null) {
            sr.setTemplateCode(message.getTemplateCode());
            if (message.getLanguage() != null) {
                sr.setLanguageCode(message.getLanguage().getCode());
            }
            sr.setParams(message.getTemplateParams());
        }
        sr.setRecipient(recipient);
        if (channel != null) {
            sr.setChannelId(channel.getId());
            sr.setChannelType(channel.getChannelType());
            sr.setChannelName(channel.getCode());
        }
        sr.setSubmittedAt(resolveSubmittedAt(task));
        sr.setStatus(SendRecordStatus.FAILED);
        sr.setErrorMessage(errorMessage);
        recordService.writeRecord(sr);
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
