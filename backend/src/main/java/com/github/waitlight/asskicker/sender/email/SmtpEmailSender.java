package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.Sender;
import com.github.waitlight.asskicker.sender.SenderProperty;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

public class SmtpEmailSender implements Sender {

    private final JavaMailSender mailSender;

    private final EmailSenderProperty property;

    private final EmailSenderProperty.Smtp smtpProperties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailSenderProperty property) {
        this.mailSender = mailSender;
        this.property = property;
        this.smtpProperties = property.getSmtp();
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
