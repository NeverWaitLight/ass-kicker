package com.github.waitlight.asskicker.manager;

import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.service.SendRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendTaskExecutor implements org.springframework.beans.factory.DisposableBean {

    private final TemplateManager templateManager;
    private final ChannelManager channelManager;
    private final SendRecordService sendRecordService;

    private ExecutorService taskExecutor;

    @PostConstruct
    public void init() {
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void execute(SendTask task) {
        taskExecutor.submit(() -> processTask(task));
    }

    public void handleRejectedTask(SendTask task, String reason) {
        markRecipientsFailed(task, null, null, fallbackRecipients(task.getRecipients()),
                "TASK_REJECTED", messageOrDefault(reason, "Task executor rejected task"));
    }

    void processTask(SendTask task) {
        List<String> recipients = normalizeRecipients(task.getRecipients());
        List<String> failureRecipients = recipients.isEmpty() ? Collections.singletonList(null) : recipients;
        String lastErrorCode = null;
        String lastErrorMessage = null;

        try {
            Language language;
            try {
                language = Language.fromCode(task.getLanguageCode());
            } catch (IllegalArgumentException ex) {
                lastErrorCode = "INVALID_LANGUAGE";
                lastErrorMessage = ex.getMessage();
                markRecipientsFailed(task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            String renderedContent;
            try {
                renderedContent = templateManager.fill(task.getTemplateCode(), language, task.getParams()).block();
            } catch (ResponseStatusException ex) {
                String reason = ex.getReason() != null ? ex.getReason() : "";
                lastErrorCode = reason.contains("Language template not found") ? "LANGUAGE_TEMPLATE_NOT_FOUND"
                        : "TEMPLATE_NOT_FOUND";
                lastErrorMessage = reason;
                markRecipientsFailed(task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            Channel channelEntity;
            try {
                channelEntity = channelManager.selectChannel(task.getTemplateCode()).block();
            } catch (Exception ex) {
                lastErrorCode = "CHANNEL_NOT_FOUND";
                lastErrorMessage = messageOrDefault(ex.getMessage(), "No available channel");
                markRecipientsFailed(task, renderedContent, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }
            if (channelEntity == null) {
                lastErrorCode = "CHANNEL_NOT_FOUND";
                lastErrorMessage = "No available channel for template: " + task.getTemplateCode();
                markRecipientsFailed(task, renderedContent, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }
            if (recipients.isEmpty()) {
                lastErrorCode = "RECIPIENTS_EMPTY";
                lastErrorMessage = "No valid recipients provided";
                markRecipientsFailed(task, renderedContent, channelEntity, failureRecipients, lastErrorCode,
                        lastErrorMessage);
                return;
            }

            com.github.waitlight.asskicker.channels.Channel<?> sendChannel = channelManager
                    .resolveChannel(channelEntity);
            if (sendChannel == null) {
                lastErrorCode = "CHANNEL_CREATE_FAILED";
                lastErrorMessage = "Unsupported channel type: " + channelEntity.getType();
                markRecipientsFailed(task, renderedContent, channelEntity, failureRecipients, lastErrorCode,
                        lastErrorMessage);
                return;
            }

            for (String recipient : recipients) {
                processRecipient(task, renderedContent, channelEntity, sendChannel, recipient);
            }
        } catch (Exception ex) {
            lastErrorCode = "CONSUMER_ERROR";
            lastErrorMessage = messageOrDefault(ex.getMessage(), "Consumer execution failed");
            markRecipientsFailed(task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
        }
    }

    private boolean processRecipient(SendTask task, String renderedContent,
            Channel channelEntity,
            com.github.waitlight.asskicker.channels.Channel<?> sendChannel,
            String recipient) {
        long sendStartedAt = Instant.now().toEpochMilli();
        MsgResp response = sendMessage(task, renderedContent, channelEntity, sendChannel, recipient, sendStartedAt);
        SendRecordStatus finalStatus = response.isSuccess() ? SendRecordStatus.SUCCESS : SendRecordStatus.FAILED;
        long sentAt = Instant.now().toEpochMilli();
        saveFinalRecord(task, renderedContent, channelEntity, recipient, finalStatus,
                response.getErrorCode(), response.getErrorMessage(), sentAt, sendStartedAt);
        return response.isSuccess();
    }

    private MsgResp sendMessage(SendTask task, String renderedContent,
            Channel channelEntity,
            com.github.waitlight.asskicker.channels.Channel<?> sendChannel,
            String recipient,
            long sendStartedAt) {
        MsgReq request = MsgReq.builder()
                .recipient(recipient)
                .subject("")
                .content(renderedContent)
                .attributes(Map.of("senderType", channelEntity.getType().name()))
                .build();
        MsgResp response;
        try {
            response = sendChannel.send(request);
        } catch (Exception ex) {
            response = MsgResp.failure("SEND_EXCEPTION", messageOrDefault(ex.getMessage(), "Send failed"));
        }
        return response;
    }

    private void markRecipientsFailed(SendTask task,
            String renderedContent,
            Channel channelEntity,
            List<String> recipients,
            String errorCode,
            String errorMessage) {
        for (String recipient : recipients) {
            long failedAt = Instant.now().toEpochMilli();
            saveFinalRecord(task, renderedContent, channelEntity, recipient, SendRecordStatus.FAILED,
                    errorCode, errorMessage, failedAt, 0L);
        }
    }

    private void saveFinalRecord(SendTask task,
            String renderedContent,
            Channel channelEntity,
            String recipient,
            SendRecordStatus status,
            String errorCode,
            String errorMessage,
            long sentAt,
            long startedAt) {
        try {
            SendRecord record = buildFinalRecord(task, renderedContent, channelEntity, recipient,
                    status, errorCode, errorMessage, sentAt);
            sendRecordService.writeRecord(record);
        } catch (Exception ex) {
            log.warn("SEND_RECORD_SAVE_FAILED taskId={} recipient={} status={} errorCode={} errorMessage={}",
                    task.getTaskId(), recipient, SendRecordStatus.FAILED, "RECORD_SAVE_FAILED",
                    messageOrDefault(ex.getMessage(), "Failed to save send record"));
        }
    }

    private SendRecord buildFinalRecord(SendTask task, String renderedContent, Channel channelEntity,
            String recipient, SendRecordStatus status, String errorCode, String errorMessage, long sentAt) {
        SendRecord record = new SendRecord();
        record.setTaskId(task.getTaskId());
        record.setTemplateCode(task.getTemplateCode());
        record.setLanguageCode(task.getLanguageCode());
        record.setParams(task.getParams());
        record.setChannelId(channelEntity != null ? channelEntity.getId() : null);
        record.setRecipients(task.getRecipients());
        record.setRecipient(recipient);
        record.setSubmittedAt(task.getSubmittedAt());
        record.setRenderedContent(renderedContent);
        record.setChannelType(channelEntity != null ? channelEntity.getType() : null);
        record.setChannelName(channelEntity != null ? channelEntity.getName() : null);
        record.setStatus(status);
        record.setErrorCode(status == SendRecordStatus.SUCCESS ? null : errorCode);
        record.setErrorMessage(status == SendRecordStatus.SUCCESS ? null : errorMessage);
        record.setSentAt(sentAt);
        return record;
    }

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
