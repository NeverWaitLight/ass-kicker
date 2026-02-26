package com.github.waitlight.asskicker.kafka;

import com.github.waitlight.asskicker.model.SendTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Component
public class SendTaskProducer {

    private static final Logger logger = LoggerFactory.getLogger(SendTaskProducer.class);

    private final KafkaTemplate<String, SendTask> kafkaTemplate;

    public SendTaskProducer(KafkaTemplate<String, SendTask> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<SendTask> publish(SendTask task) {
        CompletableFuture<SendResult<String, SendTask>> future =
                kafkaTemplate.send(KafkaConfig.SEND_TASKS_TOPIC, task.getTaskId(), task);
        return Mono.fromFuture(future)
                .doOnSuccess(result -> logger.debug("SendTask published taskId={}", task.getTaskId()))
                .doOnError(ex -> logger.warn("SendTask publish failed taskId={} reason={}", task.getTaskId(), ex.getMessage()))
                .thenReturn(task);
    }
}
