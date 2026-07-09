package com.github.waitlight.asskicker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.waitlight.asskicker.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.waitlight.asskicker.channel.ChannelFactory;
import com.github.waitlight.asskicker.channel.ChannelManager;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.service.Sender;
import com.github.waitlight.asskicker.service.TemplateEngine;

import com.github.waitlight.asskicker.service.ChannelService;
import com.github.waitlight.asskicker.service.RecordService;

import org.bson.types.ObjectId;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SenderTest {

        @Mock
        private TemplateEngine templateEngine;

        @Mock
        private RecordService recordService;

        @Mock
        private ChannelService channelService;

        @Mock
        private ChannelFactory channelFactory;

        @Mock
        private ChannelManager channelManager;

        private static UniMessage buildTemplate() {
                UniMessage message = new UniMessage();
                message.setTemplateCode("sms-template");
                message.setLanguage(Language.ZH_CN);
                return message;
        }

        @Test
        void send_generatesTaskIdWhenMissingOrBlank() {
                UniMessage template = buildTemplate();
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fillold(any(UniMessage.class))).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, recordService);
                UniTask missingId = UniTask.builder()
                                .message(template)
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(missingId))
                                .expectNextMatches(id -> id != null && !id.isBlank())
                                .verifyComplete();
                assertThat(missingId.getTaskId()).isNotBlank();
                assertThat(ObjectId.isValid(missingId.getTaskId())).isTrue();

                UniTask blankId = UniTask.builder()
                                .message(template)
                                .taskId("   ")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(blankId))
                                .expectNextMatches(id -> id != null && !id.isBlank())
                                .verifyComplete();
                assertThat(blankId.getTaskId()).isNotBlank();
                assertThat(ObjectId.isValid(blankId.getTaskId())).isTrue();
                verify(channelManager, timeout(5000).times(2)).chose(any(ChannelType.class), anyString());
        }

        @Test
        void send_preservesExistingTaskIdAndFillsSubmittedAtWhenMissing() {
                UniMessage template = buildTemplate();
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fillold(any(UniMessage.class))).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, recordService);
                UniTask task = UniTask.builder()
                                .message(template)
                                .taskId("fixed-task-id")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("fixed-task-id")
                                .verifyComplete();
                assertThat(task.getTaskId()).isEqualTo("fixed-task-id");
                assertThat(task.getSubmittedAt()).isNotNull();
                verify(channelManager, timeout(5000)).chose(any(ChannelType.class), anyString());
        }

        @Test
        void send_writesFailedRecordWhenTemplateFillReturnsNull() {
                when(templateEngine.fillold(any(UniMessage.class))).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, recordService);
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-fill-null")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123", "+14155550124"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-fill-null")
                                .verifyComplete();

                ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
                verify(recordService, timeout(5000).times(1)).create(captor.capture());

                RecordEntity record = captor.getValue();
                assertThat(record.getStatus()).isEqualTo(com.github.waitlight.asskicker.model.SendRecordStatus.FAILED);
                assertThat(record.getRecipient()).isNull();
                assertThat(record.getErrorMessage()).isNotBlank();
        }

        @Test
        void send_writesFailedRecordWhenTemplateFillThrows() {
                when(templateEngine.fillold(any(UniMessage.class)))
                                .thenReturn(Mono.error(new RuntimeException("template-engine-error")));

                Sender sender = new Sender(templateEngine, channelManager, recordService);
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-fill-throws")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(Set.of("+14155550123"))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-fill-throws")
                                .verifyComplete();

                ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
                verify(recordService, timeout(5000).times(1)).create(captor.capture());

                RecordEntity record = captor.getValue();
                assertThat(record.getStatus()).isEqualTo(com.github.waitlight.asskicker.model.SendRecordStatus.FAILED);
                assertThat(record.getRecipient()).isNull();
                assertThat(record.getErrorMessage()).contains("template-engine-error");
        }

        @Test
        void send_writesFailedRecordPerRecipientWhenChannelNotFound() {
                UniMessage rendered = buildTemplate();
                rendered.setContent("ok");
                when(templateEngine.fillold(any(UniMessage.class))).thenReturn(Mono.just(rendered));
                when(channelManager.chose(any(ChannelType.class), anyString())).thenReturn(Mono.empty());

                Sender sender = new Sender(templateEngine, channelManager, recordService);
                String r1 = "+14155550123";
                String r2 = "+14155550124";
                UniTask task = UniTask.builder()
                                .message(buildTemplate())
                                .taskId("task-no-channel")
                                .address(UniAddress.builder()
                                                .channelType(ChannelType.SMS)
                                                .recipients(new LinkedHashSet<>(List.of(r1, r2)))
                                                .build())
                                .build();

                StepVerifier.create(sender.send(task))
                                .expectNext("task-no-channel")
                                .verifyComplete();

                ArgumentCaptor<RecordEntity> captor = ArgumentCaptor.forClass(RecordEntity.class);
                verify(recordService, timeout(5000).times(2)).create(captor.capture());

                assertThat(captor.getAllValues())
                                .extracting(RecordEntity::getRecipient,
                                                RecordEntity::getStatus)
                                .containsExactlyInAnyOrder(
                                                tuple(r1, com.github.waitlight.asskicker.model.SendRecordStatus.FAILED),
                                                tuple(r2, com.github.waitlight.asskicker.model.SendRecordStatus.FAILED));
                assertThat(captor.getAllValues())
                                .allMatch(r -> r.getErrorMessage() != null && !r.getErrorMessage().isBlank());
        }
}
