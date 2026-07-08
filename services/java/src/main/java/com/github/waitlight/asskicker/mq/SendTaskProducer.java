package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.config.RocketMQConfig;
import com.github.waitlight.asskicker.dto.UniTask;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SendTaskProducer {

    private static final Logger logger = LoggerFactory.getLogger(SendTaskProducer.class);

    private final RocketMQTemplate rocketMQTemplate;

    public SendTaskProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * @deprecated 随 {@link com.github.waitlight.asskicker.dto.UniTask} 废弃而废弃。
     */
    @Deprecated
    public Mono<UniTask> publish(UniTask task) {
        return Mono.fromCallable(() -> {
            rocketMQTemplate.syncSend(
                    RocketMQConfig.SEND_TASKS_TOPIC,
                    MessageBuilder.withPayload(task).build()
            );
            logger.debug("SendTask published taskId={}", task.getTaskId());
            return task;
        }).onErrorResume(ex -> {
            logger.warn("SendTask publish failed taskId={} reason={}",
                    task.getTaskId(), ex.getMessage());
            return Mono.error(ex);
        });
    }
}
