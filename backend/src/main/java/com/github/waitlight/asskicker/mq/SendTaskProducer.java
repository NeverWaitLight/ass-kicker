package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.dto.UniSendReq;
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

    private final KafkaTemplate<String, UniSendReq> kafkaTemplate;

    public SendTaskProducer(KafkaTemplate<String, UniSendReq> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<UniSendReq> publish(UniSendReq task) {
        CompletableFuture<SendResult<String, UniSendReq>> future =
                kafkaTemplate.send(KafkaConfig.SEND_TASKS_TOPIC, task.getTaskId(), task);
        return Mono.fromFuture(future)
                .doOnSuccess(result -> logger.debug("SendTask published taskId={}", task.getTaskId()))
                .doOnError(ex -> logger.warn("SendTask publish failed taskId={} reason={}",
                        task.getTaskId(), ex.getMessage()))
                .thenReturn(task);
    }
}
