package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.ChannelDebugSimulator;
import com.github.waitlight.asskicker.channel.ChannelProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmailChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugSimulator debugSimulator;

    public EmailChannelFactory(WebClient sharedWebClient, ChannelDebugSimulator debugSimulator) {
        this.sharedWebClient = sharedWebClient;
        this.debugSimulator = debugSimulator;
    }

    public EmailChannel<?> create(ChannelProperty config) {
        if (config instanceof HttpEmailChannelProperty http) {
            return new HttpEmailChannel(http, sharedWebClient, debugSimulator);
        }
        if (config instanceof SmtpEmailChannelProperty smtp) {
            return new SmtpEmailChannel(smtp, debugSimulator);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
