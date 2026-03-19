package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmailChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugProperties debugProperties;

    public EmailChannelFactory(WebClient sharedWebClient, ChannelDebugProperties debugProperties) {
        this.sharedWebClient = sharedWebClient;
        this.debugProperties = debugProperties;
    }

    public Channel<?> create(ChannelProperties config) {
        if (config instanceof HttpEmailChannelProperties http) {
            return new HttpEmailChannel(http, sharedWebClient, debugProperties);
        }
        if (config instanceof SmtpEmailChannelProperties smtp) {
            return new SmtpEmailChannel(smtp, debugProperties);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
