package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IMChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugSimulator debugSimulator;

    public IMChannelFactory(WebClient sharedWebClient, ChannelDebugSimulator debugSimulator) {
        this.sharedWebClient = sharedWebClient;
        this.debugSimulator = debugSimulator;
    }

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof DingTalkIMChannelConfig dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient, debugSimulator);
        }
        if (config instanceof WechatWorkIMChannelConfig wechatWork) {
            return new WechatWorkIMChannel(wechatWork, sharedWebClient, debugSimulator);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
