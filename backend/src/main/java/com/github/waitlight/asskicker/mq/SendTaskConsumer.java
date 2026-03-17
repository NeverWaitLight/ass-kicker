package com.github.waitlight.asskicker.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.channels.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channels.push.PushChannelFactory;
import com.github.waitlight.asskicker.channels.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.channels.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.manager.ChannelManager;
import com.github.waitlight.asskicker.manager.TemplateManager;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class SendTaskConsumer implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(SendTaskConsumer.class);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TemplateManager templateManager;
    private final ChannelManager channelManager;
    private final SendRecordRepository sendRecordRepository;
    private final EmailChannelFactory emailChannelFactory;
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelFactory imChannelFactory;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelFactory pushChannelFactory;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelFactory smsChannelFactory;
    private final SmsChannelConfigConverter smsChannelConfigConverter;
    private final ObjectMapper objectMapper;
    private final ExecutorService taskExecutor;

    @Autowired
    public SendTaskConsumer(TemplateManager templateManager,
            ChannelManager channelManager,
            SendRecordRepository sendRecordRepository,
            EmailChannelFactory emailChannelFactory,
            EmailChannelConfigConverter emailChannelConfigConverter,
            IMChannelFactory imChannelFactory,
            IMChannelConfigConverter imChannelConfigConverter,
            PushChannelFactory pushChannelFactory,
            PushChannelConfigConverter pushChannelConfigConverter,
            SmsChannelFactory smsChannelFactory,
            SmsChannelConfigConverter smsChannelConfigConverter,
            ObjectMapper objectMapper) {
        this(templateManager, channelManager, sendRecordRepository,
                emailChannelFactory, emailChannelConfigConverter, imChannelFactory, imChannelConfigConverter,
                pushChannelFactory, pushChannelConfigConverter, smsChannelFactory, smsChannelConfigConverter,
                objectMapper, Executors.newVirtualThreadPerTaskExecutor());
    }

    SendTaskConsumer(TemplateManager templateManager,
            ChannelManager channelManager,
            SendRecordRepository sendRecordRepository,
            EmailChannelFactory emailChannelFactory,
            EmailChannelConfigConverter emailChannelConfigConverter,
            IMChannelFactory imChannelFactory,
            IMChannelConfigConverter imChannelConfigConverter,
            PushChannelFactory pushChannelFactory,
            PushChannelConfigConverter pushChannelConfigConverter,
            SmsChannelFactory smsChannelFactory,
            SmsChannelConfigConverter smsChannelConfigConverter,
            ObjectMapper objectMapper,
            ExecutorService taskExecutor) {
        this.templateManager = templateManager;
        this.channelManager = channelManager;
        this.sendRecordRepository = sendRecordRepository;
        this.emailChannelFactory = emailChannelFactory;
        this.emailChannelConfigConverter = emailChannelConfigConverter;
        this.imChannelFactory = imChannelFactory;
        this.imChannelConfigConverter = imChannelConfigConverter;
        this.pushChannelFactory = pushChannelFactory;
        this.pushChannelConfigConverter = pushChannelConfigConverter;
        this.smsChannelFactory = smsChannelFactory;
        this.smsChannelConfigConverter = smsChannelConfigConverter;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
    }

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(SendTask task) {
        if (task == null || task.getTaskId() == null) {
            logger.warn("SendTaskConsumer ignored null or empty task");
            return;
        }
        try {
            submitTask(task);
        } catch (RejectedExecutionException ex) {
            logger.error("SendTaskConsumer rejected taskId={} reason={}", task.getTaskId(), ex.getMessage());
            markRecipientsFailed(task, null, null, fallbackRecipients(task.getRecipients()),
                    "TASK_REJECTED", messageOrDefault(ex.getMessage(), "Task executor rejected task"));
        }
    }

    public void submitTask(SendTask task) {
        taskExecutor.submit(() -> processTask(task));
    }

    public void processTask(SendTask task) {
        long taskStartedAt = Instant.now().toEpochMilli();
        List<String> recipients = normalizeRecipients(task.getRecipients());
        List<String> failureRecipients = recipients.isEmpty() ? Collections.singletonList(null) : recipients;
        int successCount = 0;
        int failureCount = 0;
        String lastErrorCode = null;
        String lastErrorMessage = null;

        try {
            Language language;
            try {
                language = Language.fromCode(task.getLanguageCode());
            } catch (IllegalArgumentException ex) {
                failureCount = failureRecipients.size();
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
                failureCount = failureRecipients.size();
                markRecipientsFailed(task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            Channel channelEntity;
            try {
                channelEntity = channelManager.selectChannel(task.getTemplateCode()).block();
            } catch (Exception ex) {
                lastErrorCode = "CHANNEL_NOT_FOUND";
                lastErrorMessage = messageOrDefault(ex.getMessage(), "No available channel");
                failureCount = failureRecipients.size();
                markRecipientsFailed(task, renderedContent, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }
            if (channelEntity == null) {
                lastErrorCode = "CHANNEL_NOT_FOUND";
                lastErrorMessage = "No available channel for template: " + task.getTemplateCode();
                failureCount = failureRecipients.size();
                markRecipientsFailed(task, renderedContent, null, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }
            if (recipients.isEmpty()) {
                lastErrorCode = "RECIPIENTS_EMPTY";
                lastErrorMessage = "No valid recipients provided";
                failureCount = failureRecipients.size();
                markRecipientsFailed(task, renderedContent, channelEntity, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            com.github.waitlight.asskicker.channels.Channel<?> sendChannel = createChannel(channelEntity);
            if (sendChannel == null) {
                lastErrorCode = "CHANNEL_CREATE_FAILED";
                lastErrorMessage = "Unsupported channel type: " + channelEntity.getType();
                failureCount = failureRecipients.size();
                markRecipientsFailed(task, renderedContent, channelEntity, failureRecipients, lastErrorCode, lastErrorMessage);
                return;
            }

            try {
                for (String recipient : recipients) {
                    boolean success = processRecipient(task, renderedContent, channelEntity, sendChannel, recipient);
                    if (success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }
            } finally {
                closeChannel(sendChannel);
            }
        } catch (Exception ex) {
            lastErrorCode = "CONSUMER_ERROR";
            lastErrorMessage = messageOrDefault(ex.getMessage(), "Consumer execution failed");
            failureCount = failureRecipients.size();
            markRecipientsFailed(task, null, null, failureRecipients, lastErrorCode, lastErrorMessage);
        } finally {
            long durationMs = Math.max(0L, Instant.now().toEpochMilli() - taskStartedAt);
            int totalCount = successCount + failureCount;
            SendRecordStatus overallStatus = successCount > 0 ? SendRecordStatus.SUCCESS : SendRecordStatus.FAILED;
            if (overallStatus == SendRecordStatus.FAILED) {
                logger.warn("SEND_TASK_COMPLETED taskId={} total={} success={} failed={} status={} errorCode={} errorMessage={} durationMs={}",
                        task.getTaskId(), totalCount, successCount, failureCount, overallStatus,
                        lastErrorCode, lastErrorMessage, durationMs);
            } else {
                logger.info("SEND_TASK_COMPLETED taskId={} total={} success={} failed={} status={} durationMs={}",
                        task.getTaskId(), totalCount, successCount, failureCount, overallStatus, durationMs);
            }
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
            SendRecord saved = sendRecordRepository.save(record).block();
            if (saved == null || saved.getId() == null) {
                logger.warn("SEND_RECORD_SAVE_FAILED taskId={} recipient={} status={} errorCode={} errorMessage={}",
                        task.getTaskId(), recipient, SendRecordStatus.FAILED, "RECORD_SAVE_FAILED",
                        "Failed to save send record");
            }
        } catch (Exception ex) {
            logger.warn("SEND_RECORD_SAVE_FAILED taskId={} recipient={} status={} errorCode={} errorMessage={}",
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

    private com.github.waitlight.asskicker.channels.Channel<?> createChannel(Channel channelEntity) {
        ChannelType type = channelEntity.getType();
        Map<String, Object> properties = readProperties(channelEntity.getPropertiesJson());
        if (type == ChannelType.EMAIL) {
            ChannelConfig config = emailChannelConfigConverter.fromProperties(properties);
            return emailChannelFactory.create(config);
        }
        if (type == ChannelType.IM) {
            ChannelConfig config = imChannelConfigConverter.fromProperties(properties);
            return imChannelFactory.create(config);
        }
        if (type == ChannelType.PUSH) {
            ChannelConfig config = pushChannelConfigConverter.fromProperties(properties);
            return pushChannelFactory.create(config);
        }
        if (type == ChannelType.SMS) {
            ChannelConfig config = smsChannelConfigConverter.fromProperties(properties);
            return smsChannelFactory.create(config);
        }
        return null;
    }

    private void closeChannel(com.github.waitlight.asskicker.channels.Channel<?> channel) {
        if (channel instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                logger.warn("SEND_CHANNEL_CLOSE_FAILED errorCode={} errorMessage={}",
                        "CHANNEL_CLOSE_FAILED", messageOrDefault(ex.getMessage(), "Failed to close channel"));
            }
        }
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            logger.warn("SEND_CHANNEL_PROPERTIES_PARSE_FAILED errorCode={} errorMessage={}",
                    "CHANNEL_PROPERTIES_PARSE_FAILED",
                    messageOrDefault(ex.getMessage(), "Failed to parse channel properties"));
            return new LinkedHashMap<>();
        }
    }

    private String messageOrDefault(String message, String defaultMessage) {
        if (message == null || message.isBlank()) {
            return defaultMessage;
        }
        return message;
    }

    @Override
    public void destroy() {
        shutdownExecutor(taskExecutor, "taskExecutor");
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
            logger.warn("Executor shutdown interrupted name={}", name);
        }
    }
}
