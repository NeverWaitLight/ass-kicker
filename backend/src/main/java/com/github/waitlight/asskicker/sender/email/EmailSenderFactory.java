package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.Sender;
import com.github.waitlight.asskicker.sender.SenderConfig;
import org.springframework.stereotype.Component;

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
