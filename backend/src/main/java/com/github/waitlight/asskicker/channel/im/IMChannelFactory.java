package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channel.ChannelProperty;
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

    public Channel<?> create(ChannelProperty config) {
        if (config instanceof DingTalkIMChannelProperty dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient, debugSimulator);
        }
        if (config instanceof WeComIMChannelProperty wechatWork) {
            return new WeComIMChannel(wechatWork, sharedWebClient, debugSimulator);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
