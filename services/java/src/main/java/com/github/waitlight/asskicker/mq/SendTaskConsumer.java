package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.config.RocketMQConfig;
import com.github.waitlight.asskicker.dto.UniTask;
import com.github.waitlight.asskicker.service.Sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@RocketMQMessageListener(
        topic = RocketMQConfig.SEND_TASKS_TOPIC,
        consumerGroup = "ass-kicker-consumer-group"
)
public class SendTaskConsumer implements RocketMQListener<UniTask> {

    private final Sender sender;

    /**
     * @deprecated 随 {@link com.github.waitlight.asskicker.dto.UniTask} 废弃而废弃。
     */
    @Deprecated
    @Override
    public void onMessage(UniTask task) {
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
