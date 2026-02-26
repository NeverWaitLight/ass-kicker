package com.github.waitlight.asskicker.channels.email;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.channels.ChannelConfig;

@Component
public class EmailChannelFactory {

    public EmailChannel<?> create(ChannelConfig config) {
        if (config instanceof HttpEmailChannelConfig http) {
            return new HttpEmailChannel(http);
        }
        if (config instanceof SmtpEmailChannelConfig smtp) {
            return new SmtpEmailChannel(smtp);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
