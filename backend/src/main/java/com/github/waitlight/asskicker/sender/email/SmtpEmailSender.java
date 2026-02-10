package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.Sender;
import com.github.waitlight.asskicker.sender.SenderProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SmtpEmailSender implements Sender {

    private final JavaMailSender mailSender;

    private final SmtpEmailSenderProperty property;

    private final SmtpEmailSenderProperty smtpProperties;

    public SmtpEmailSender(JavaMailSender mailSender, SmtpEmailSenderProperty property) {
        this.mailSender = mailSender;
        this.property = property;
        this.smtpProperties = property;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (request == null) {
            return MessageResponse.failure("INVALID_REQUEST", "Message request is null");
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts <= smtpProperties.getMaxRetries()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(String.valueOf(resolveFrom()));
                helper.setTo(String.valueOf(request.getRecipient()));
                helper.setSubject(String.valueOf(request.getSubject()));
                helper.setText(String.valueOf(request.getContent()), false);
                mailSender.send(message);
                return MessageResponse.success(message.getMessageID());
            } catch (MailException | MessagingException ex) {
                lastException = ex;
                attempts++;
                if (attempts <= smtpProperties.getMaxRetries()) {
                    try {
                        Thread.sleep(smtpProperties.getRetryDelay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return MessageResponse.failure("MAIL_SEND_INTERRUPTED", ie.getMessage());
                    }
                }
            }
        }

        return MessageResponse.failure("MAIL_SEND_FAILED",
                lastException != null ? lastException.getMessage() : "Unknown error after " + attempts + " attempts");
    }

    private String resolveFrom() {
        if (smtpProperties.getFrom() != null && !smtpProperties.getFrom().isBlank()) {
            return smtpProperties.getFrom();
        }
        return smtpProperties.getUsername();
    }

    @Override
    public SenderProperty getProperty() {
        return property;
    }
}

@Data
class SmtpEmailSenderProperty implements SenderProperty {

    @NotBlank
    private String host;

    @Min(1)
    private int port = 465;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String protocol = "smtp";

    private boolean sslEnabled = true;

    private String from;

    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);

    @Min(0)
    private int maxRetries = 3;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(1);
}
