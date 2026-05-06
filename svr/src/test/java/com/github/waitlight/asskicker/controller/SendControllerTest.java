package com.github.waitlight.asskicker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.waitlight.asskicker.TemplateEngine;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.mq.SendTaskProducer;
import com.github.waitlight.asskicker.model.Language;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SendControllerTest {

    @Mock
    private SendTaskProducer sendTaskProducer;

    @Mock
    private TemplateEngine templateEngine;

    private SendController controller;

    @BeforeEach
    void setUp() {
        controller = new SendController(sendTaskProducer, templateEngine);
    }

    @Test
    void send_whenTemplateVariablesMissing_rejectsWithoutPublishing() {
        UniTask task = validTask();
        when(templateEngine.findMissingVariables(any(UniMessage.class)))
                .thenReturn(Mono.just(new LinkedHashSet<>(List.of("name", "code"))));

        StepVerifier.create(controller.send(task))
                .assertNext(resp -> {
                    assertThat(resp.code()).isEqualTo("400");
                    assertThat(resp.message()).isEqualTo("模板变量未填充: name, code");
                    assertThat(resp.data()).isNull();
                })
                .verifyComplete();

        verify(sendTaskProducer, never()).publish(any(UniTask.class));
    }

    @Test
    void submit_whenTemplateVariablesMissing_rejectsWithoutPublishing() {
        UniTask task = validTask();
        when(templateEngine.findMissingVariables(any(UniMessage.class)))
                .thenReturn(Mono.just(new LinkedHashSet<>(List.of("name"))));

        StepVerifier.create(controller.submit(task))
                .assertNext(resp -> {
                    assertThat(resp.code()).isEqualTo("400");
                    assertThat(resp.message()).isEqualTo("模板变量未填充: name");
                    assertThat(resp.data()).isNull();
                })
                .verifyComplete();

        verify(sendTaskProducer, never()).publish(any(UniTask.class));
    }

    @Test
    void send_whenTemplateVariablesComplete_publishesTask() {
        UniTask task = validTask();
        when(templateEngine.findMissingVariables(any(UniMessage.class))).thenReturn(Mono.just(Collections.emptySet()));
        when(sendTaskProducer.publish(any(UniTask.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(controller.send(task))
                .assertNext(resp -> {
                    assertThat(resp.code()).isEqualTo("200");
                    assertThat(resp.data().taskId()).isNotBlank();
                })
                .verifyComplete();

        verify(sendTaskProducer).publish(any(UniTask.class));
    }

    @Test
    void send_whenTemplateMissing_rejectsWithoutPublishing() {
        UniTask task = validTask();
        when(templateEngine.findMissingVariables(any(UniMessage.class))).thenReturn(Mono.empty());

        StepVerifier.create(controller.send(task))
                .assertNext(resp -> {
                    assertThat(resp.code()).isEqualTo("400");
                    assertThat(resp.message()).isEqualTo("模板不存在或语言内容不存在");
                    assertThat(resp.data()).isNull();
                })
                .verifyComplete();

        verify(sendTaskProducer, never()).publish(any(UniTask.class));
    }

    private UniTask validTask() {
        UniMessage message = new UniMessage();
        message.setTemplateCode("welcome");
        message.setLanguage(Language.ZH_CN);

        return UniTask.builder()
                .message(message)
                .address(UniAddress.ofSms("13800138000"))
                .build();
    }
}
