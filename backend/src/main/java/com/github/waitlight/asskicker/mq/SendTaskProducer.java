package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.dto.send.SendRequest;
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

    private final KafkaTemplate<String, SendRequest> kafkaTemplate;

    public SendTaskProducer(KafkaTemplate<String, SendRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<SendRequest> publish(SendRequest task) {
        CompletableFuture<SendResult<String, SendRequest>> future =
                kafkaTemplate.send(KafkaConfig.SEND_TASKS_TOPIC, task.taskId(), task);
        return Mono.fromFuture(future)
                .doOnSuccess(result -> logger.debug("SendTask published taskId={}", task.taskId()))
                .doOnError(ex -> logger.warn("SendTask publish failed taskId={} reason={}", task.taskId(), ex.getMessage()))
                .thenReturn(task);
    }
}
