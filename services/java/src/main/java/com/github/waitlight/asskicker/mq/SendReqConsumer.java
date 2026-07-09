package com.github.waitlight.asskicker.mq;

import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.config.RocketMQConfig;
import com.github.waitlight.asskicker.service.Sender;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQConfig.SEND_REQS_TOPIC,
        consumerGroup = "ass-kicker-send-req-consumer-group"
)
public class SendReqConsumer implements RocketMQListener<SendReq> {

    private final Sender sender;

    @Override
    public void onMessage(SendReq req) {
        if (req == null || req.getType() == null) {
            return;
        }
        sender.send(req).block();
    }
}
