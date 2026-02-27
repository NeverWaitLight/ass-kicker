package com.github.waitlight.asskicker.channels.email;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channels.ChannelConfig;

@Component
public class EmailChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugSimulator debugSimulator;

    public EmailChannelFactory(WebClient sharedWebClient, ChannelDebugSimulator debugSimulator) {
        this.sharedWebClient = sharedWebClient;
        this.debugSimulator = debugSimulator;
    }

    public EmailChannel<?> create(ChannelConfig config) {
        if (config instanceof HttpEmailChannelConfig http) {
            return new HttpEmailChannel(http, sharedWebClient, debugSimulator);
        }
        if (config instanceof SmtpEmailChannelConfig smtp) {
            return new SmtpEmailChannel(smtp, debugSimulator);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
