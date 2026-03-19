package com.github.waitlight.asskicker.channel.im;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IMChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugProperties debugProperties;

    public IMChannelFactory(WebClient sharedWebClient, ChannelDebugProperties debugProperties) {
        this.sharedWebClient = sharedWebClient;
        this.debugProperties = debugProperties;
    }

    public Channel<?> create(ChannelProperties config) {
        if (config instanceof DingTalkIMChannelProperties dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient, debugProperties);
        }
        if (config instanceof WeComIMChannelProperties wechatWork) {
            return new WeComIMChannel(wechatWork, sharedWebClient, debugProperties);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
