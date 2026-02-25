package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageRequest;
import com.github.waitlight.asskicker.sender.MessageResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public class SmtpEmailSender extends EmailSender<SmtpEmailSenderConfig> {

    private final JavaMailSender mailSender;

    public SmtpEmailSender(SmtpEmailSenderConfig config) {
        super(config);
        this.mailSender = buildJavaMailSender(config);
    }

    private JavaMailSender buildJavaMailSender(SmtpEmailSenderConfig smtp) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setUsername(smtp.getUsername());
        mailSender.setPassword(smtp.getPassword());
        String protocol = smtp.getProtocol().name().toLowerCase(Locale.ROOT);
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(smtp.getConnectionTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(smtp.getReadTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(smtp.getReadTimeout().toMillis()));
        if (smtp.isSslEnabled()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.ssl.trust", smtp.getHost());
        }
        return mailSender;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        if (request == null) {
            return MessageResponse.failure("INVALID_REQUEST", "Message request is null");
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts <= config.getMaxRetries()) {
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
                if (attempts <= config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryDelay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return MessageResponse.failure("MAIL_SEND_INTERRUPTED", ie.getMessage());
                    }
                }
            }
        }

        return MessageResponse.failure("MAIL_SEND_FAILED", lastException != null ? lastException.getMessage() : "Unknown error after " + attempts + " attempts");
    }

    private String resolveFrom() {
        if (config.getFrom() != null && !config.getFrom().isBlank()) {
            return config.getFrom();
        }
        return config.getUsername();
    }

}
