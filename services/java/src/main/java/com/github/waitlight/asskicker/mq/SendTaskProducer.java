package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.config.RocketMQConfig;
import com.github.waitlight.asskicker.dto.UniTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SendTaskProducer {

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
            log.debug("SendTask published taskId={}", task.getTaskId());
            return task;
        }).onErrorResume(ex -> {
            log.warn("SendTask publish failed taskId={} reason={}",
                    task.getTaskId(), ex.getMessage());
            return Mono.error(ex);
        });
    }
}
