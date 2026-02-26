package com.github.waitlight.asskicker.channels.email;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.github.waitlight.asskicker.channels.MessageRequest;
import com.github.waitlight.asskicker.channels.MessageResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

public class SmtpEmailChannel extends EmailChannel<SmtpEmailChannelConfig> {

    private final JavaMailSender mailSender;

    public SmtpEmailChannel(SmtpEmailChannelConfig config) {
        super(config);
        this.mailSender = buildJavaMailChannel(config);
    }

    private JavaMailSender buildJavaMailChannel(SmtpEmailChannelConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        String protocol = config.getProtocol().name().toLowerCase(Locale.ROOT);
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(config.getConnectionTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(config.getReadTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(config.getReadTimeout().toMillis()));
        if (config.isSslEnabled()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.ssl.trust", config.getHost());
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
