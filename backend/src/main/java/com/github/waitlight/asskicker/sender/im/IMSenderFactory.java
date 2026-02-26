package com.github.waitlight.asskicker.sender.im;

import com.github.waitlight.asskicker.sender.Sender;
import com.github.waitlight.asskicker.sender.SenderConfig;
import org.springframework.stereotype.Component;

@Component
public class IMSenderFactory {

    public Sender<?> create(SenderConfig config) {
        if (config instanceof DingTalkIMSenderConfig dingTalk) {
            return new DingTalkIMSender(dingTalk);
        }
        if (config instanceof WechatWorkIMSenderConfig wechatWork) {
            return new WechatWorkIMSender(wechatWork);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
