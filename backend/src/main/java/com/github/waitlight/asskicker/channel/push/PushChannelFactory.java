package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PushChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugProperties debugProperties;

    public PushChannelFactory(WebClient sharedWebClient, ChannelDebugProperties debugProperties) {
        this.sharedWebClient = sharedWebClient;
        this.debugProperties = debugProperties;
    }

    public Channel<?> create(ChannelProperties config) {
        if (config instanceof APNsPushChannelProperties apns) {
            return new APNsPushChannel(apns, debugProperties);
        }
        if (config instanceof FCMPushChannelProperties fcm) {
            return new FCMPushChannel(fcm, sharedWebClient, debugProperties);
        }
        throw new IllegalArgumentException("Unsupported push channel config: " + config);
    }
}
