package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class IMChannelFactory {

    private final WebClient sharedWebClient;

    public IMChannelFactory(WebClient sharedWebClient) {
        this.sharedWebClient = sharedWebClient;
    }

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof DingTalkIMChannelConfig dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient);
        }
        if (config instanceof WechatWorkIMChannelConfig wechatWork) {
            return new WechatWorkIMChannel(wechatWork, sharedWebClient);
        }

        throw new IllegalArgumentException("Unsupported IM sender config: " + config);
    }
}
