package com.github.waitlight.asskicker.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.MsgReq;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channels.push.PushChannelFactory;
import com.github.waitlight.asskicker.channels.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channels.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.channels.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.LanguageTemplate;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.model.Template;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.LanguageTemplateRepository;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import com.github.waitlight.asskicker.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SendTaskConsumer {

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
    }

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(SendTask task) {
        if (task == null || task.getTaskId() == null) {
            logger.warn("SendTaskConsumer ignored null or empty task");
            return;
        }
        logger.info("SendTaskConsumer processing taskId={}", task.getTaskId());
        try {
            Template template = templateRepository.findByCode(task.getTemplateCode()).block();
            if (template == null) {
                writeFailureRecord(task, null, null, "TEMPLATE_NOT_FOUND", "模板不存在: " + task.getTemplateCode());
                return;
            }
            Language language;
            try {
                language = Language.fromCode(task.getLanguageCode());
            } catch (IllegalArgumentException e) {
                writeFailureRecord(task, null, null, "INVALID_LANGUAGE", e.getMessage());
                return;
            }
            LanguageTemplate langTemplate = languageTemplateRepository
                    .findByTemplateIdAndLanguage(template.getId(), language).block();
            if (langTemplate == null) {
                writeFailureRecord(task, null, null, "LANGUAGE_TEMPLATE_NOT_FOUND", "语言模板不存在");
                return;
            }
            String renderedContent = render(langTemplate.getContent(), task.getParams());

            Channel channelEntity = channelRepository.findById(task.getChannelId()).block();
            if (channelEntity == null) {
                writeFailureRecord(task, renderedContent, null, "CHANNEL_NOT_FOUND", "通道不存在: " + task.getChannelId());
                return;
            }
            com.github.waitlight.asskicker.channels.Channel<?> sendChannel = createChannel(channelEntity);
            if (sendChannel == null) {
                writeFailureRecord(task, renderedContent, channelEntity.getName(), "CHANNEL_CREATE_FAILED", "不支持的通道类型: " + channelEntity.getType());
                return;
            }
            try {
                List<String> recipients = task.getRecipients() != null ? task.getRecipients() : List.of();
                for (String recipient : recipients) {
                    sendAndRecord(task, renderedContent, channelEntity, sendChannel, recipient);
                }
            } finally {
                closeChannel(sendChannel);
            }
        } catch (Exception ex) {
            logger.error("SendTaskConsumer failed taskId={}", task.getTaskId(), ex);
            writeFailureRecord(task, null, null, "CONSUMER_ERROR", ex.getMessage());
        }
    }

    private void sendAndRecord(SendTask task, String renderedContent,
                               Channel channelEntity,
                               com.github.waitlight.asskicker.channels.Channel<?> sendChannel,
                               String recipient) {
        long sentAt = Instant.now().toEpochMilli();
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
            logger.warn("Send failed taskId={} recipient={} reason={}", task.getTaskId(), recipient, ex.getMessage());
            response = MsgResp.failure("SEND_EXCEPTION", ex.getMessage());
        }
        SendRecord record = buildRecord(task, renderedContent, channelEntity, recipient, response, sentAt);
        sendRecordRepository.save(record).block();
    }

    private SendRecord buildRecord(SendTask task, String renderedContent, Channel channelEntity,
                                   String recipient, MsgResp response, long sentAt) {
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
        record.setChannelType(channelEntity.getType());
        record.setChannelName(channelEntity.getName());
        record.setSuccess(response.isSuccess());
        record.setErrorCode(response.getErrorCode());
        record.setErrorMessage(response.getErrorMessage());
        record.setSentAt(sentAt);
        return record;
    }

    private void writeFailureRecord(SendTask task, String renderedContent, String channelName,
                                    String errorCode, String errorMessage) {
        SendRecord record = new SendRecord();
        record.setTaskId(task.getTaskId());
        record.setTemplateCode(task.getTemplateCode());
        record.setLanguageCode(task.getLanguageCode());
        record.setParams(task.getParams());
        record.setChannelId(task.getChannelId());
        record.setRecipients(task.getRecipients());
        record.setRecipient(task.getRecipients() != null && !task.getRecipients().isEmpty() ? task.getRecipients().get(0) : null);
        record.setSubmittedAt(task.getSubmittedAt());
        record.setRenderedContent(renderedContent);
        record.setChannelName(channelName);
        record.setSuccess(false);
        record.setErrorCode(errorCode);
        record.setErrorMessage(errorMessage);
        record.setSentAt(Instant.now().toEpochMilli());
        sendRecordRepository.save(record).block();
    }

    private String render(String content, Map<String, Object> params) {
        if (content == null) return "";
        if (params == null || params.isEmpty()) return content;
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
                logger.warn("SendTaskConsumer close channel failed reason={}", ex.getMessage());
            }
        }
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            logger.warn("Failed to read channel properties: {}", ex.getMessage());
            return new LinkedHashMap<>();
        }
    }
}
