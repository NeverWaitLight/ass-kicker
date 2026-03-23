package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.dto.send.SendRequest;
import com.github.waitlight.asskicker.manager.SendTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendTaskConsumer {

    private final SendTaskExecutor sendTaskExecutor;

    @KafkaListener(topics = KafkaConfig.SEND_TASKS_TOPIC, containerFactory = "sendTaskListenerContainerFactory")
    public void consume(SendRequest task) {
        if (task == null || task.taskId() == null) {
            log.warn("SendTaskConsumer ignored null or empty task");
            return;
        }
        try {
            sendTaskExecutor.submit(task);
        } catch (RejectedExecutionException ex) {
            log.error("SendTaskConsumer rejected taskId={} reason={}", task.taskId(), ex.getMessage());
            sendTaskExecutor.handleRejectedTask(task,
                    ex.getMessage() != null ? ex.getMessage() : "Task executor rejected task");
        }
    }
}
