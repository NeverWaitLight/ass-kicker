package com.github.waitlight.asskicker.channels.email;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.waitlight.asskicker.channels.ChannelConfig;

@Component
public class EmailChannelFactory {

    private final WebClient sharedWebClient;

    public EmailChannelFactory(WebClient sharedWebClient) {
        this.sharedWebClient = sharedWebClient;
    }

    public EmailChannel<?> create(ChannelConfig config) {
        if (config instanceof HttpEmailChannelConfig http) {
            return new HttpEmailChannel(http, sharedWebClient);
        }
        if (config instanceof SmtpEmailChannelConfig smtp) {
            return new SmtpEmailChannel(smtp);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
