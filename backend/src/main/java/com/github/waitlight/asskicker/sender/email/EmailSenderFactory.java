package com.github.waitlight.asskicker.sender.email;

import org.springframework.stereotype.Component;

import com.github.waitlight.asskicker.sender.SenderConfig;

@Component
public class EmailSenderFactory {

    public EmailSender<?> create(SenderConfig config) {
        if (config instanceof HttpEmailSenderConfig http) {
            return new HttpEmailSender(http);
        }
        if (config instanceof SmtpEmailSenderConfig smtp) {
            return new SmtpEmailSender(smtp);
        }

        throw new IllegalArgumentException("Unsupported email sender property: " + config);
    }
}
