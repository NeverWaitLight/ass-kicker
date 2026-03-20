package com.github.waitlight.asskicker.manager;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.MsgReq;
import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.dto.template.FilledTemplateResult;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.SendRecordEntity;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.service.SendRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendTaskExecutor implements DisposableBean {

    private final TemplateManager templateManager;
    private final ChannelManager channelManager;
    private final SendRecordService sendRecordService;

    @Value("${send-record.ttl-days:90}")
    private int sendRecordTtlDays;

    private ExecutorService taskExecutor;

    @PostConstruct
    public void init() {
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Submit the send task to the task executor.
     *
     * @param task
     */
    public void submit(SendTask task) {
        taskExecutor.submit(() -> process(task));
    }

    public void handleRejectedTask(SendTask task, String reason) {
        markRecipientsFailed(
                task,
                null,
                null,
                fallbackRecipients(task.getRecipients()),
                "TASK_REJECTED",
                messageOrDefault(reason, "Task executor rejected task"));
    }

    /**
     * Process the send task.
     *
     * @param task
     */
    private void process(SendTask task) {
        List<String> recipients = normalizeRecipients(task.getRecipients());
        List<String> failureRecipients = recipients.isEmpty() ? Collections.singletonList(null) : recipients;
        String lastErrorCode = null;
        String lastErrorMessage = null;

        try {
            Language language = task.getLanguage();
            if (language == null) {
                lastErrorCode = "INVALID_LANGUAGE";
                lastErrorMessage = "Language is required";
                markRecipientsFailed(
                        task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            final FilledTemplateResult filledResult;
            try {
                filledResult = templateManager
                        .fill(task.getTemplateCode(), language, task.getParams())
                        .block();
            } catch (ResponseStatusException ex) {
                String reason = ex.getReason() != null ? ex.getReason() : "";
                lastErrorCode = reason.contains("Language template not found")
                        ? "LANGUAGE_TEMPLATE_NOT_FOUND"
                        : "TEMPLATE_NOT_FOUND";
                lastErrorMessage = reason;
                markRecipientsFailed(
                        task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            String renderedContent = filledResult.renderedContent();
            ChannelType channelType = filledResult.template().getChannelType();

            if (recipients.isEmpty()) {
                lastErrorCode = "RECIPIENTS_EMPTY";
                lastErrorMessage = "No valid recipients provided";
                markRecipientsFailed(
                        task,
                        renderedContent,
                        channelType,
                        failureRecipients,
                        lastErrorCode,
                        lastErrorMessage);
                return;
            }

            if (channelType == null) {
                lastErrorCode = "CHANNEL_NOT_FOUND";
                lastErrorMessage = "Template has no channel type: " + task.getTemplateCode();
                markRecipientsFailed(
                        task,
                        renderedContent,
                        null,
                        failureRecipients,
                        lastErrorCode,
                        lastErrorMessage);
                return;
            }

            for (String recipient : recipients) {
                Channel<?> channel;
                try {
                    channel = channelManager.selectChannel(channelType, recipient).block();
                } catch (Exception ex) {
                    markRecipientsFailed(
                            task,
                            renderedContent,
                            channelType,
                            List.of(recipient),
                            "CHANNEL_NOT_FOUND",
                            messageOrDefault(ex.getMessage(), "No available channel"));
                    continue;
                }

                if (channel == null) {
                    markRecipientsFailed(
                            task,
                            renderedContent,
                            channelType,
                            List.of(recipient),
                            "CHANNEL_NOT_FOUND",
                            "No available channel for type: " + channelType);
                    continue;
                }

                processRecipient(task, renderedContent, channelType, channel, recipient);
            }
        } catch (Exception ex) {
            lastErrorCode = "CONSUMER_ERROR";
            lastErrorMessage = messageOrDefault(ex.getMessage(), "Consumer execution failed");
            markRecipientsFailed(
                    task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
        }
    }

    private boolean processRecipient(
            SendTask task,
            String renderedContent,
            ChannelType channelType,
            Channel<?> sendChannel,
            String recipient) {
        long sendStartedAt = Instant.now().toEpochMilli();
        MsgResp response = sendMessage(
                task, renderedContent, sendChannel, recipient, sendStartedAt);
        SendRecordStatus finalStatus = response.isSuccess() ? SendRecordStatus.SUCCESS : SendRecordStatus.FAILED;
        long sentAt = Instant.now().toEpochMilli();
        saveFinalRecord(
                task,
                renderedContent,
                channelType,
                recipient,
                finalStatus,
                response.getErrorCode(),
                response.getErrorMessage(),
                sentAt,
                sendStartedAt);
        return response.isSuccess();
    }

    private MsgResp sendMessage(
            SendTask task,
            String renderedContent,
            Channel<?> sendChannel,
            String recipient,
            long sendStartedAt) {
        MsgReq request = new MsgReq(recipient, "", renderedContent, null);
        MsgResp response;
        try {
            response = sendChannel.send(request);
        } catch (Exception ex) {
            response = MsgResp.failure(
                    "SEND_EXCEPTION", messageOrDefault(ex.getMessage(), "Send failed"));
        }
        return response;
    }

    private void markRecipientsFailed(
            SendTask task,
            String renderedContent,
            ChannelType channelType,
            List<String> recipients,
            String errorCode,
            String errorMessage) {
        for (String recipient : recipients) {
            long failedAt = Instant.now().toEpochMilli();
            saveFinalRecord(
                    task,
                    renderedContent,
                    channelType,
                    recipient,
                    SendRecordStatus.FAILED,
                    errorCode,
                    errorMessage,
                    failedAt,
                    0L);
        }
    }

    private void saveFinalRecord(
            SendTask task,
            String renderedContent,
            ChannelType channelType,
            String recipient,
            SendRecordStatus status,
            String errorCode,
            String errorMessage,
            long sentAt,
            long startedAt) {
        try {
            SendRecordEntity record = buildFinalRecord(
                    task,
                    renderedContent,
                    channelType,
                    recipient,
                    status,
                    errorCode,
                    errorMessage,
                    sentAt);
            sendRecordService.writeRecord(record);
        } catch (Exception ex) {
            log.warn(
                    "SEND_RECORD_SAVE_FAILED taskId={} recipient={} status={} errorCode={} errorMessage={}",
                    task.getTaskId(),
                    recipient,
                    SendRecordStatus.FAILED,
                    "RECORD_SAVE_FAILED",
                    messageOrDefault(ex.getMessage(), "Failed to save send record"));
        }
    }

    private SendRecordEntity buildFinalRecord(
            SendTask task,
            String renderedContent,
            ChannelType channelType,
            String recipient,
            SendRecordStatus status,
            String errorCode,
            String errorMessage,
            long sentAt) {
        SendRecordEntity record = new SendRecordEntity();
        record.setTaskId(task.getTaskId());
        record.setTemplateCode(task.getTemplateCode());
        record.setLanguageCode(
                task.getLanguage() != null ? task.getLanguage().getCode() : null);
        record.setParams(task.getParams());
        record.setRecipient(recipient);
        record.setSubmittedAt(task.getSubmittedAt());
        record.setRenderedContent(renderedContent);
        record.setChannelType(channelType);
        record.setStatus(status);
        record.setErrorCode(status == SendRecordStatus.SUCCESS ? null : errorCode);
        record.setErrorMessage(status == SendRecordStatus.SUCCESS ? null : errorMessage);
        record.setSentAt(sentAt);
        record.setExpireAt(Instant.now().plus(Duration.ofDays(sendRecordTtlDays)));
        return record;
    }

    /**
     * Normalize recipients list by trimming and filtering out empty values.
     *
     * @param recipients
     * @return normalized recipients list
     */
    private List<String> normalizeRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        return recipients.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private List<String> fallbackRecipients(List<String> recipients) {
        List<String> normalized = normalizeRecipients(recipients);
        if (normalized.isEmpty()) {
            return Collections.singletonList(null);
        }
        return normalized;
    }

    private String messageOrDefault(String message, String defaultMessage) {
        if (message == null || message.isBlank()) {
            return defaultMessage;
        }
        return message;
    }

    @Override
    public void destroy() {
        if (taskExecutor != null) {
            shutdownExecutor(taskExecutor, "taskExecutor");
        }
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            log.warn("Executor shutdown interrupted name={}", name);
        }
    }
}
