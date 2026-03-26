package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.dto.send.SendRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskConsumer {

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(SendRequest task) {
        // TODO: replace with new async send processing logic after migration.
        if (task == null || task.taskId() == null) {
            log.warn("SendTaskConsumer ignored null or empty task");
            return;
        }
        log.info("SendTaskConsumer placeholder consume taskId={}", task.taskId());
    }
}
