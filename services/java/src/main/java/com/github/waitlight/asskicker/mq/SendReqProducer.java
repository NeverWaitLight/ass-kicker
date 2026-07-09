package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.RocketMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SendReqProducer {

    private final RocketMQTemplate template;

    public SendReqProducer(RocketMQTemplate template) {
        this.template = template;
    }

    public Mono<String> publish(SendReq req) {
        return Mono.fromCallable(() -> {
            template.syncSend(
                    RocketMQConfig.SEND_REQS_TOPIC,
                    MessageBuilder.withPayload(req).build()
            );
            return req.getRecordId();
        }).onErrorResume(Mono::error);
    }
}
