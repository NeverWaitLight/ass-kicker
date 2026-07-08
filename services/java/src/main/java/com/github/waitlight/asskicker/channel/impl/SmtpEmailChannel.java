package com.github.waitlight.asskicker.channel.impl;

import java.util.List;

import com.github.waitlight.asskicker.channel.AbstractChannel;
import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.model.ChannelEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Channel(type = ChannelType.EMAIL, provider = ChannelProvider.SMTP, reqType = EmailReq.class)
public class SmtpEmailChannel extends AbstractChannel<EmailReq> {

    private final Properties properties;
    private final JavaMailSender mailSender;

    public SmtpEmailChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = objectMapper.convertValue(provider.getProperties(), Properties.class);
        this.mailSender = buildMailSender(this.properties);
    }

    @Override
    protected Mono<String> doSend(EmailReq req) {
        return Mono.defer(() -> {
            List<String> recipients = req.getTo();
            if (recipients == null || recipients.isEmpty()) {
                return Mono.error(new IllegalArgumentException("SMTP recipients (to) required"));
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(StringUtils.defaultIfBlank(req.getFrom(), properties.getFrom()));
            message.setTo(recipients.toArray(String[]::new));
            if (req.getCc() != null && !req.getCc().isEmpty()) {
                message.setCc(req.getCc().toArray(String[]::new));
            }
            if (req.getBcc() != null && !req.getBcc().isEmpty()) {
                message.setBcc(req.getBcc().toArray(String[]::new));
            }
            message.setSubject(StringUtils.defaultString(req.getSubject()));
            message.setText(StringUtils.defaultString(req.getBody()));

            return Mono.fromRunnable(() -> mailSender.send(message))
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn("SMTP ok " + recipients.size() + " recipient(s)");
        });
    }

    @Override
    public void dispose() {
    }

    private static JavaMailSender buildMailSender(Properties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding("UTF-8");

        java.util.Properties mailProps = sender.getJavaMailProperties();
        mailProps.put("mail.smtp.auth", "true");
        if (properties.getPort() == 465) {
            mailProps.put("mail.smtp.ssl.enable", "true");
        } else {
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.starttls.required", "true");
        }
        return sender;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Properties {

        /** SMTP 服务器主机名,如 smtp.gmail.com */
        @NotBlank
        private String host;

        /** SMTP 端口:465 走 SSL,其他端口走 STARTTLS(默认 587) */
        @Min(1)
        @Max(65535)
        private int port = 587;

        /** SMTP 登录用户名,通常与 from 邮箱相同 */
        @NotBlank
        private String username;

        /** SMTP 登录密码或应用专用密码 */
        private String password;

        /** 默认发件人地址,当 EmailReq.from 为空时使用 */
        @NotBlank
        @Email
        private String from;
    }
}
