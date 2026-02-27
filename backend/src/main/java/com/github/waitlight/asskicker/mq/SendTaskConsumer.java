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
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SendTaskConsumer implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(SendTaskConsumer.class);
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    private final TemplateRepository templateRepository;
    private final LanguageTemplateRepository languageTemplateRepository;
    private final ChannelRepository channelRepository;
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
    private final ExecutorService auditExecutor;

    @Autowired
    public SendTaskConsumer(TemplateRepository templateRepository,
                            LanguageTemplateRepository languageTemplateRepository,
                            ChannelRepository channelRepository,
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
        this(templateRepository, languageTemplateRepository, channelRepository, sendRecordRepository,
                emailChannelFactory, emailChannelConfigConverter, imChannelFactory, imChannelConfigConverter,
                pushChannelFactory, pushChannelConfigConverter, smsChannelFactory, smsChannelConfigConverter,
                objectMapper, Executors.newVirtualThreadPerTaskExecutor(), Executors.newVirtualThreadPerTaskExecutor());
    }

    SendTaskConsumer(TemplateRepository templateRepository,
                     LanguageTemplateRepository languageTemplateRepository,
                     ChannelRepository channelRepository,
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
                     ExecutorService taskExecutor,
                     ExecutorService auditExecutor) {
        this.templateRepository = templateRepository;
        this.languageTemplateRepository = languageTemplateRepository;
        this.channelRepository = channelRepository;
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
        this.auditExecutor = auditExecutor;
    }

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(SendTask task) {
        if (task == null || task.getTaskId() == null) {
            logger.warn("SendTaskConsumer ignored null or empty task");
            return;
        }
        try {
            taskExecutor.submit(() -> processTask(task));
        } catch (RejectedExecutionException ex) {
            logger.error("SendTaskConsumer rejected taskId={} reason={}", task.getTaskId(), ex.getMessage());
            markRecipientsFailedAsync(task, null, null, fallbackRecipients(task.getRecipients()),
                    "TASK_REJECTED", messageOrDefault(ex.getMessage(), "Task executor rejected task"));
        }
    }

    private void processTask(SendTask task) {
        long taskStartedAt = Instant.now().toEpochMilli();
        List<String> recipients = normalizeRecipients(task.getRecipients());
        List<String> failureRecipients = recipients.isEmpty() ? Collections.singletonList(null) : recipients;
        logExecutionAsync("SEND_TASK_RECEIVED", task.getTaskId(), null, null, SendRecordStatus.PENDING, null, null, 0L);
        try {
            Template template = templateRepository.findByCode(task.getTemplateCode()).block();
            if (template == null) {
                markRecipientsFailedAsync(task, null, null, failureRecipients,
                        "TEMPLATE_NOT_FOUND", "Template not found: " + task.getTemplateCode());
                return;
            }

            Language language;
            try {
                language = Language.fromCode(task.getLanguageCode());
            } catch (IllegalArgumentException ex) {
                markRecipientsFailedAsync(task, null, null, failureRecipients, "INVALID_LANGUAGE", ex.getMessage());
                return;
            }

            LanguageTemplate langTemplate = languageTemplateRepository
                    .findByTemplateIdAndLanguage(template.getId(), language).block();
            if (langTemplate == null) {
                markRecipientsFailedAsync(task, null, null, failureRecipients,
                        "LANGUAGE_TEMPLATE_NOT_FOUND", "Language template not found");
                return;
            }
            String renderedContent = render(langTemplate.getContent(), task.getParams());

            Channel channelEntity = channelRepository.findById(task.getChannelId()).block();
            if (channelEntity == null) {
                markRecipientsFailedAsync(task, renderedContent, null, failureRecipients,
                        "CHANNEL_NOT_FOUND", "Channel not found: " + task.getChannelId());
                return;
            }
            if (recipients.isEmpty()) {
                markRecipientsFailedAsync(task, renderedContent, channelEntity, failureRecipients,
                        "RECIPIENTS_EMPTY", "No valid recipients provided");
                return;
            }

            com.github.waitlight.asskicker.channels.Channel<?> sendChannel = createChannel(channelEntity);
            if (sendChannel == null) {
                markRecipientsFailedAsync(task, renderedContent, channelEntity, failureRecipients,
                        "CHANNEL_CREATE_FAILED", "Unsupported channel type: " + channelEntity.getType());
                return;
            }

            try {
                for (String recipient : recipients) {
                    processRecipient(task, renderedContent, channelEntity, sendChannel, recipient);
                }
            } finally {
                closeChannel(sendChannel);
            }
        } catch (Exception ex) {
            markRecipientsFailedAsync(task, null, null, failureRecipients,
                    "CONSUMER_ERROR", messageOrDefault(ex.getMessage(), "Consumer execution failed"));
        } finally {
            long durationMs = Math.max(0L, Instant.now().toEpochMilli() - taskStartedAt);
            logExecutionAsync("SEND_TASK_FINISHED", task.getTaskId(), null, null, SendRecordStatus.PENDING, null, null, durationMs);
        }
    }

    private void processRecipient(SendTask task, String renderedContent,
                                  Channel channelEntity,
                                  com.github.waitlight.asskicker.channels.Channel<?> sendChannel,
                                  String recipient) {
        long sendStartedAt = Instant.now().toEpochMilli();
        CompletableFuture<String> pendingRecordFuture = createPendingRecordAsync(task, renderedContent, channelEntity, recipient);
        MsgResp response = sendMessage(task, renderedContent, channelEntity, sendChannel, recipient, sendStartedAt);
        SendRecordStatus finalStatus = response.isSuccess() ? SendRecordStatus.SUCCESS : SendRecordStatus.FAILED;
        long sentAt = Instant.now().toEpochMilli();

        pendingRecordFuture.whenComplete((recordId, pendingEx) -> {
            if (pendingEx != null) {
                logExecutionAsync("SEND_RECORD_PENDING_FAILED", task.getTaskId(), null, recipient, SendRecordStatus.FAILED,
                        "PENDING_RECORD_CREATE_FAILED",
                        messageOrDefault(pendingEx.getMessage(), "Failed to create pending record"),
                        Math.max(0L, sentAt - sendStartedAt));
                return;
            }
            updateRecordStatusAsync(task.getTaskId(), recordId, recipient, finalStatus,
                    response.getErrorCode(), response.getErrorMessage(), sentAt, sendStartedAt);
        });
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
        SendRecordStatus status = response.isSuccess() ? SendRecordStatus.SUCCESS : SendRecordStatus.FAILED;
        long durationMs = Math.max(0L, Instant.now().toEpochMilli() - sendStartedAt);
        logExecutionAsync("SEND_RESULT", task.getTaskId(), null, recipient, status,
                response.getErrorCode(), response.getErrorMessage(), durationMs);
        return response;
    }

    private void markRecipientsFailedAsync(SendTask task,
                                           String renderedContent,
                                           Channel channelEntity,
                                           List<String> recipients,
                                           String errorCode,
                                           String errorMessage) {
        for (String recipient : recipients) {
            long failedAt = Instant.now().toEpochMilli();
            CompletableFuture<String> pendingRecordFuture = createPendingRecordAsync(task, renderedContent, channelEntity, recipient);
            pendingRecordFuture.whenComplete((recordId, pendingEx) -> {
                if (pendingEx != null) {
                    logExecutionAsync("SEND_RECORD_PENDING_FAILED", task.getTaskId(), null, recipient, SendRecordStatus.FAILED,
                            errorCode, messageOrDefault(pendingEx.getMessage(), errorMessage), 0L);
                    return;
                }
                updateRecordStatusAsync(task.getTaskId(), recordId, recipient, SendRecordStatus.FAILED,
                        errorCode, errorMessage, failedAt, 0L);
            });
            logExecutionAsync("SEND_RESULT", task.getTaskId(), null, recipient, SendRecordStatus.FAILED, errorCode, errorMessage, 0L);
        }
    }

    private CompletableFuture<String> createPendingRecordAsync(SendTask task,
                                                               String renderedContent,
                                                               Channel channelEntity,
                                                               String recipient) {
        return CompletableFuture.supplyAsync(() -> {
            SendRecord record = buildPendingRecord(task, renderedContent, channelEntity, recipient);
            SendRecord saved = sendRecordRepository.save(record).block();
            if (saved == null || saved.getId() == null) {
                throw new IllegalStateException("Failed to create pending send record");
            }
            logExecution("SEND_RECORD_PENDING_CREATED", task.getTaskId(), saved.getId(), recipient,
                    SendRecordStatus.PENDING, null, null, 0L);
            return saved.getId();
        }, auditExecutor);
    }

    private SendRecord buildPendingRecord(SendTask task, String renderedContent, Channel channelEntity, String recipient) {
        SendRecord record = new SendRecord();
        record.setTaskId(task.getTaskId());
        record.setTemplateCode(task.getTemplateCode());
        record.setLanguageCode(task.getLanguageCode());
        record.setParams(task.getParams());
        record.setChannelId(task.getChannelId());
        record.setRecipients(task.getRecipients());
        record.setRecipient(recipient);
        record.setSubmittedAt(task.getSubmittedAt());
        record.setRenderedContent(renderedContent);
        record.setChannelType(channelEntity != null ? channelEntity.getType() : null);
        record.setChannelName(channelEntity != null ? channelEntity.getName() : null);
        record.setStatus(SendRecordStatus.PENDING);
        record.setErrorCode(null);
        record.setErrorMessage(null);
        record.setSentAt(null);
        return record;
    }

    private void updateRecordStatusAsync(String taskId,
                                         String recordId,
                                         String recipient,
                                         SendRecordStatus status,
                                         String errorCode,
                                         String errorMessage,
                                         long sentAt,
                                         long startedAt) {
        CompletableFuture.runAsync(() -> {
            try {
                SendRecord record = sendRecordRepository.findById(recordId).block();
                if (record == null) {
                    logExecution("SEND_RECORD_NOT_FOUND", taskId, recordId, recipient, SendRecordStatus.FAILED,
                            "RECORD_NOT_FOUND", "Send record not found", 0L);
                    return;
                }
                record.setStatus(status);
                if (status == SendRecordStatus.SUCCESS) {
                    record.setErrorCode(null);
                    record.setErrorMessage(null);
                } else {
                    record.setErrorCode(errorCode);
                    record.setErrorMessage(errorMessage);
                }
                record.setSentAt(sentAt);
                sendRecordRepository.save(record).block();
                long durationMs = startedAt == 0L ? 0L : Math.max(0L, sentAt - startedAt);
                logExecution("SEND_RECORD_STATUS_UPDATED", taskId, recordId, recipient, status,
                        record.getErrorCode(), record.getErrorMessage(), durationMs);
            } catch (Exception ex) {
                logExecution("SEND_RECORD_STATUS_UPDATE_FAILED", taskId, recordId, recipient, SendRecordStatus.FAILED,
                        "STATUS_UPDATE_FAILED", messageOrDefault(ex.getMessage(), "Failed to update send record"), 0L);
            }
        }, auditExecutor);
    }

    private void logExecutionAsync(String event,
                                   String taskId,
                                   String recordId,
                                   String recipient,
                                   SendRecordStatus status,
                                   String errorCode,
                                   String errorMessage,
                                   long durationMs) {
        CompletableFuture.runAsync(() -> logExecution(event, taskId, recordId, recipient, status, errorCode, errorMessage, durationMs),
                auditExecutor);
    }

    private void logExecution(String event,
                              String taskId,
                              String recordId,
                              String recipient,
                              SendRecordStatus status,
                              String errorCode,
                              String errorMessage,
                              long durationMs) {
        if (status == SendRecordStatus.FAILED) {
            logger.warn("{} taskId={} recordId={} recipient={} status={} errorCode={} errorMessage={} durationMs={}",
                    event, taskId, recordId, recipient, status, errorCode, errorMessage, durationMs);
            return;
        }
        logger.info("{} taskId={} recordId={} recipient={} status={} errorCode={} errorMessage={} durationMs={}",
                event, taskId, recordId, recipient, status, errorCode, errorMessage, durationMs);
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

    private String render(String content, Map<String, Object> params) {
        if (content == null) {
            return "";
        }
        if (params == null || params.isEmpty()) {
            return content;
        }
        Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = params.get(key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
                logExecutionAsync("SEND_CHANNEL_CLOSE_FAILED", null, null, null, SendRecordStatus.FAILED,
                        "CHANNEL_CLOSE_FAILED", messageOrDefault(ex.getMessage(), "Failed to close channel"), 0L);
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
            logExecutionAsync("SEND_CHANNEL_PROPERTIES_PARSE_FAILED", null, null, null, SendRecordStatus.FAILED,
                    "CHANNEL_PROPERTIES_PARSE_FAILED", messageOrDefault(ex.getMessage(), "Failed to parse channel properties"), 0L);
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
        shutdownExecutor(auditExecutor, "auditExecutor");
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
