package com.github.waitlight.asskicker.channel.push;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channel.ChannelProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PushChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugSimulator debugSimulator;

    public PushChannelFactory(WebClient sharedWebClient, ChannelDebugSimulator debugSimulator) {
        this.sharedWebClient = sharedWebClient;
        this.debugSimulator = debugSimulator;
    }

    public Channel<?> create(ChannelProperty config) {
        if (config instanceof APNsPushChannelProperty apns) {
            return new APNsPushChannel(apns, debugSimulator);
        }
        if (config instanceof FCMPushChannelProperty fcm) {
            return new FCMPushChannel(fcm, sharedWebClient, debugSimulator);
        }
        throw new IllegalArgumentException("Unsupported push channel config: " + config);
    }
}
