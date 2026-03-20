package com.github.waitlight.asskicker.channel.email;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.MsgReq;
import com.github.waitlight.asskicker.channel.MsgResp;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmtpEmailChannel extends Channel<SmtpEmailChannelSpec> {

    private final JavaMailSender mailSender;

    public SmtpEmailChannel(SmtpEmailChannelSpec spec, ChannelDebugProperties debugProperties) {
        super(spec, debugProperties);
        this.mailSender = debugProperties.isEnabled() ? null : buildJavaMailChannel(spec);
    }

    private JavaMailSender buildJavaMailChannel(SmtpEmailChannelSpec spec) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(spec.getHost());
        mailSender.setPort(spec.getPort());
        mailSender.setUsername(spec.getUsername());
        mailSender.setPassword(spec.getPassword());
        String protocol = "smtp";
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(spec.getConnectionTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.timeout", String.valueOf(spec.getReadTimeout().toMillis()));
        javaMailProperties.put("mail.smtp.writetimeout", String.valueOf(spec.getReadTimeout().toMillis()));
        if (spec.isSslEnabled()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            javaMailProperties.put("mail.smtp.ssl.trust", spec.getHost());
        }
        return mailSender;
    }

    @Override
    protected MsgResp doSend(MsgReq request) {
        if (request == null) {
            return MsgResp.failure("INVALID_REQUEST", "Message request is null");
        }

        int attempts = 0;
        Exception lastException = null;

        while (attempts <= spec.getMaxRetries()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
                helper.setFrom(String.valueOf(resolveFrom()));
                helper.setTo(String.valueOf(request.recipient()));
                helper.setSubject(String.valueOf(request.subject()));
                helper.setText(String.valueOf(request.content()), false);
                mailSender.send(message);
                return MsgResp.success(message.getMessageID());
            } catch (MailException | MessagingException ex) {
                lastException = ex;
                attempts++;
                if (attempts <= spec.getMaxRetries()) {
                    try {
                        Thread.sleep(spec.getRetryDelay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return MsgResp.failure("MAIL_SEND_INTERRUPTED", ie.getMessage());
                    }
                }
            }
        }

        return MsgResp.failure("MAIL_SEND_FAILED", lastException != null ? lastException.getMessage() : "Unknown error after " + attempts + " attempts");
    }

    private String resolveFrom() {
        if (spec.getFrom() != null && !spec.getFrom().isBlank()) {
            return spec.getFrom();
        }
        return spec.getUsername();
    }

}
