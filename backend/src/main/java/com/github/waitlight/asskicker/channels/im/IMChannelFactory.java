package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.stereotype.Component;

@Component
public class IMChannelFactory {

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof DingTalkIMChannelConfig dingTalk) {
            return new DingTalkIMChannel(dingTalk);
        }
        if (config instanceof WechatWorkIMChannelConfig wechatWork) {
            return new WechatWorkIMChannel(wechatWork);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
