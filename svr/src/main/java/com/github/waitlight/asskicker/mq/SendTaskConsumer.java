package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.Sender;
import com.github.waitlight.asskicker.dto.UniTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskConsumer {

    private final Sender sender;

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(UniTask task) {
        if (task == null || task.getMessage() == null || task.getAddress() == null) {
            log.warn("SendTaskConsumer ignored invalid task");
            return;
        }
        sender.send(task)
                .doOnSuccess(result -> log.info("SendTaskConsumer consumed taskId={} result={}",
                        task.getTaskId(), result))
                .doOnError(ex -> log.warn("SendTaskConsumer failed taskId={} reason={}",
                        task.getTaskId(), ex.getMessage(), ex))
                .block();
    }
}
