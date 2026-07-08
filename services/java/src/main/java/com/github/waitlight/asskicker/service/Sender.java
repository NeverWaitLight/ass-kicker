package com.github.waitlight.asskicker.service;

import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class Sender {

    private final TemplateEngine templateEngine;
    private final ChannelManager channelManager;

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

    public <T extends SendReq> Mono<String> send(T req) {
        if (req == null || req.getType() == null) {
            return Mono.empty();
        }
        return templateEngine.fill(req)
                .flatMap(r -> channelManager.chose(r)
                        .flatMap(channel -> invokeChannelSend(channel, r)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Mono<String> invokeChannelSend(AbstractChannel<?> channel, SendReq req) {
        return ((AbstractChannel) channel).send(req);
    }

    /**
     * @deprecated 改用 {@link #send(SendReq)}
     */
    @Deprecated
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

    /**
     * @deprecated 随 {@link UniTask} 废弃而废弃。
     */
    @Deprecated
    private void normalize(UniTask task) {
        if (task.getTaskId() == null || task.getTaskId().isBlank()) {
            task.setTaskId(ObjectId.get().toString());
        }
        if (task.getSubmittedAt() == null) {
            task.setSubmittedAt(Instant.now().toEpochMilli());
        }
    }

    /**
     * @deprecated 随 {@link UniTask} 废弃而废弃。
     */
    @Deprecated
    private void process(UniTask task) {
        UniMessage filled = null;
        try {
            filled = templateEngine.fillold(task.getMessage()).block();
            if (filled == null) {
                log.warn("Template fill returned null for taskId={}", task.getTaskId());
                return;
            }
        } catch (Exception e) {
            log.error("Failed to fill message template taskId={}", task.getTaskId(), e);
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
        AbstractChannel channel = null;
        try {
            UniAddress singleAddr = buildSingleRecipientAddress(baseCtx.getTask().getAddress(), recipient);
            channel = channelManager.chose(singleAddr.getChannelType(), recipient).block();
            if (channel == null) {
                log.warn("No channel available for recipient={} taskId={}", recipient, baseCtx.getTask().getTaskId());
                return;
            }

            SendContext ctx = baseCtx.fork(recipient, channel, singleAddr);

            UniTask sendTask = UniTask.builder()
                    .message(ctx.getUniMessage())
                    .address(ctx.getSingleAddress())
                    .taskId(ctx.getTask().getTaskId())
                    .submittedAt(ctx.getTask().getSubmittedAt())
                    .build();

            String result = channel.send(sendTask).block().toString();
            ctx.setSendResult(result);
        } catch (Exception e) {
            log.error("Failed to send to recipient={} taskId={}", recipient, baseCtx.getTask().getTaskId(), e);
        }
    }

    /**
     * @deprecated 随 {@link UniAddress} 废弃而废弃。
     */
    @Deprecated
    private UniAddress buildSingleRecipientAddress(UniAddress original, String recipient) {
        return UniAddress.builder()
                .channelType(original.getChannelType())
                .provider(original.getProvider())
                .channelKey(original.getChannelKey())
                .recipients(Set.of(recipient))
                .build();
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    private static final class SendContext {

        private final UniTask task;
        private UniMessage uniMessage;
        private AbstractChannel channel;
        private String sendResult;
        private String recipient;
        private UniAddress singleAddress;

        /**
         * @deprecated 随 {@link UniAddress} 废弃而废弃。
         */
        @Deprecated
        private SendContext fork(String recipient, AbstractChannel channel, UniAddress singleAddress) {
            SendContext forked = new SendContext(this.task);
            forked.uniMessage = this.uniMessage;
            forked.recipient = recipient;
            forked.channel = channel;
            forked.singleAddress = singleAddress;
            return forked;
        }
    }
}
