package com.github.waitlight.asskicker.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.MsgResp;
import com.github.waitlight.asskicker.channels.email.EmailChannel;
import com.github.waitlight.asskicker.channels.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channels.email.EmailChannelFactory;
import com.github.waitlight.asskicker.channels.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channels.im.IMChannelFactory;
import com.github.waitlight.asskicker.channels.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channels.push.PushChannelFactory;
import com.github.waitlight.asskicker.channels.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.channels.sms.SmsChannelFactory;
import com.github.waitlight.asskicker.manager.TemplateManager;
import com.github.waitlight.asskicker.model.Channel;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.SendRecord;
import com.github.waitlight.asskicker.model.SendRecordStatus;
import com.github.waitlight.asskicker.model.SendTask;
import com.github.waitlight.asskicker.repository.ChannelRepository;
import com.github.waitlight.asskicker.repository.SendRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendTaskConsumerTest {

    @Mock
    private TemplateManager templateManager;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private SendRecordRepository sendRecordRepository;
    @Mock
    private EmailChannelFactory emailChannelFactory;
    @Mock
    private EmailChannelConfigConverter emailChannelConfigConverter;
    @Mock
    private IMChannelFactory imChannelFactory;
    @Mock
    private IMChannelConfigConverter imChannelConfigConverter;
    @Mock
    private PushChannelFactory pushChannelFactory;
    @Mock
    private PushChannelConfigConverter pushChannelConfigConverter;
    @Mock
    private SmsChannelFactory smsChannelFactory;
    @Mock
    private SmsChannelConfigConverter smsChannelConfigConverter;
    @Mock
    private EmailChannel<?> sendChannel;

    private ExecutorService taskExecutor;
    private ExecutorService auditExecutor;
    private SendTaskConsumer consumer;

    @BeforeEach
    void setUp() {
        taskExecutor = Executors.newSingleThreadExecutor();
        auditExecutor = Executors.newSingleThreadExecutor();
        consumer = new SendTaskConsumer(
                templateManager,
                channelRepository,
                sendRecordRepository,
                emailChannelFactory,
                emailChannelConfigConverter,
                imChannelFactory,
                imChannelConfigConverter,
                pushChannelFactory,
                pushChannelConfigConverter,
                smsChannelFactory,
                smsChannelConfigConverter,
                new ObjectMapper(),
                taskExecutor,
                auditExecutor
        );
    }

    @AfterEach
    void tearDown() {
        consumer.destroy();
    }

    @Test
    void shouldUpdateRecordToFailedWhenTemplateMissing() {
        SendTask task = baseTask();
        SendRecord storedRecord = mockRecordPersistence("record-failed");

        when(templateManager.fill(any(), any(), any())).thenReturn(
                Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + task.getTemplateCode())));
        when(sendRecordRepository.findById("record-failed")).thenAnswer(invocation -> Mono.just(storedRecord));

        consumer.consume(task);

        verify(sendRecordRepository, timeout(2000)).findById("record-failed");
        ArgumentCaptor<SendRecord> captor = ArgumentCaptor.forClass(SendRecord.class);
        verify(sendRecordRepository, timeout(2000).atLeast(2)).save(captor.capture());
        SendRecord updatedRecord = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertEquals(SendRecordStatus.FAILED, updatedRecord.getStatus());
        assertEquals("TEMPLATE_NOT_FOUND", updatedRecord.getErrorCode());
    }

    @Test
    void shouldUpdateRecordToSuccessWhenSendSucceeded() {
        SendTask task = baseTask();
        Channel channel = new Channel();
        channel.setId(task.getChannelId());
        channel.setName("mail-chan");
        channel.setType(ChannelType.EMAIL);
        channel.setPropertiesJson("{}");

        SendRecord storedRecord = mockRecordPersistence("record-success");
        when(sendRecordRepository.findById("record-success")).thenAnswer(invocation -> Mono.just(storedRecord));

        when(templateManager.fill(eq(task.getTemplateCode()), eq(Language.EN), any())).thenReturn(Mono.just("hello codex"));
        when(channelRepository.findById(task.getChannelId())).thenReturn(Mono.just(channel));
        when(emailChannelConfigConverter.fromProperties(anyMap())).thenReturn(new ChannelConfig() {
        });
        when(emailChannelFactory.create(any(ChannelConfig.class))).thenAnswer(invocation -> sendChannel);
        when(sendChannel.send(any())).thenReturn(MsgResp.success("m-1"));

        consumer.consume(task);

        verify(sendRecordRepository, timeout(2000)).findById("record-success");
        ArgumentCaptor<SendRecord> captor = ArgumentCaptor.forClass(SendRecord.class);
        verify(sendRecordRepository, timeout(2000).atLeast(2)).save(captor.capture());
        SendRecord updatedRecord = captor.getAllValues().get(captor.getAllValues().size() - 1);

        assertEquals(SendRecordStatus.SUCCESS, updatedRecord.getStatus());
        assertNull(updatedRecord.getErrorCode());
        assertNull(updatedRecord.getErrorMessage());
    }

    private SendTask baseTask() {
        return SendTask.builder()
                .taskId("task-1")
                .templateCode("welcome")
                .languageCode("en")
                .params(Map.of("name", "codex"))
                .channelId("channel-1")
                .recipients(List.of("user@example.com"))
                .submittedAt(System.currentTimeMillis())
                .build();
    }

    private SendRecord mockRecordPersistence(String recordId) {
        SendRecord storedRecord = new SendRecord();
        storedRecord.setId(recordId);
        storedRecord.setStatus(SendRecordStatus.PENDING);

        when(sendRecordRepository.save(any(SendRecord.class))).thenAnswer(invocation -> {
            SendRecord record = invocation.getArgument(0);
            if (record.getId() == null) {
                record.setId(recordId);
            }
            storedRecord.setId(record.getId());
            storedRecord.setStatus(record.getStatus());
            storedRecord.setErrorCode(record.getErrorCode());
            storedRecord.setErrorMessage(record.getErrorMessage());
            storedRecord.setSentAt(record.getSentAt());
            return Mono.just(record);
        });
        return storedRecord;
    }
}
