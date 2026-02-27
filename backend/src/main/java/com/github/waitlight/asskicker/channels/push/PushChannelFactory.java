package com.github.waitlight.asskicker.channels.push;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PushChannelFactory {

    private final WebClient sharedWebClient;

    public PushChannelFactory(WebClient sharedWebClient) {
        this.sharedWebClient = sharedWebClient;
    }

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof APNsPushChannelConfig apns) {
            return new APNsPushChannel(apns);
        }
        if (config instanceof FCMPushChannelConfig fcm) {
            return new FCMPushChannel(fcm, sharedWebClient);
        }
        throw new IllegalArgumentException("Unsupported push channel config: " + config);
    }
}
