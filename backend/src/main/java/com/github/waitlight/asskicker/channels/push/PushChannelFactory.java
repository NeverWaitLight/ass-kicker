package com.github.waitlight.asskicker.channels.push;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelProperty;
import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
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
