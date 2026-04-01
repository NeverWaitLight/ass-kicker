package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.Sender;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendTaskConsumerTest {

    @Mock
    private Sender sender;

    @Test
    void consume_validTask_delegatesToSender() {
        SendTaskConsumer consumer = new SendTaskConsumer(sender);
        UniTask task = buildTask();

        when(sender.send(task)).thenReturn(Mono.just("task-001"));

        consumer.consume(task);

        verify(sender).send(task);
    }

    @Test
    void consume_nullTask_ignored() {
        SendTaskConsumer consumer = new SendTaskConsumer(sender);

        consumer.consume(null);

        verify(sender, never()).send(any());
    }

    @Test
    void consume_missingMessageOrAddress_ignored() {
        SendTaskConsumer consumer = new SendTaskConsumer(sender);
        UniTask task = buildTask();

        consumer.consume(UniTask.builder().address(task.getAddress()).build());
        consumer.consume(UniTask.builder().message(task.getMessage()).build());

        verify(sender, never()).send(any());
    }

    @Test
    void consume_senderFails_propagatesException() {
        SendTaskConsumer consumer = new SendTaskConsumer(sender);
        UniTask task = buildTask();

        when(sender.send(task)).thenReturn(Mono.error(new IllegalStateException("send failed")));

        Assertions.assertThrows(IllegalStateException.class, () -> consumer.consume(task));
        verify(sender).send(task);
    }

    private UniTask buildTask() {
        UniMessage message = new UniMessage();
        message.setTemplateCode("tpl-code");
        message.setLanguage(Language.ZH_CN);
        message.setTemplateParams(Map.of("name", "north"));

        UniAddress address = UniAddress.builder()
                .channelType(ChannelType.EMAIL)
                .channelProviderKey("provider-key")
                .recipients(Set.of("lord@winterfell.com"))
                .build();

        return UniTask.builder()
                .message(message)
                .address(address)
                .taskId("task-001")
                .submittedAt(123456789L)
                .build();
    }
}
