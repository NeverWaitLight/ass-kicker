package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import com.github.waitlight.asskicker.sender.Sender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

public class SmtpEmailSender implements Sender {

    private final JavaMailSender mailSender;

    private final EmailSenderProperties.Smtp properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailSenderProperties.Smtp properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (request == null) {
            return MessageResponse.failure("INVALID_REQUEST", "Message request is null");
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts <= properties.getMaxRetries()) {
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
                if (attempts <= properties.getMaxRetries()) {
                    try {
                        Thread.sleep(properties.getRetryDelay().toMillis());
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
        if (properties.getFrom() != null && !properties.getFrom().isBlank()) {
            return properties.getFrom();
        }
        return properties.getUsername();
    }
}

