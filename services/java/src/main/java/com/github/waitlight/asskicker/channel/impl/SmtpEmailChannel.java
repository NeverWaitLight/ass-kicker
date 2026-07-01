package com.github.waitlight.asskicker.channel.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.SendReq;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
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

public class SmtpEmailChannel extends Channel<SendReq> {

    public static final ChannelType TYPE = ChannelType.EMAIL;
    public static final ChannelProvider PROVIDER = ChannelProvider.SMTP;

    private final Properties properties;
    private final JavaMailSender mailSender;

    public SmtpEmailChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = Properties.fromProperties(provider.getProperties());
        this.mailSender = buildMailSender(this.properties);
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress);
            validateSpec();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom().trim());
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject(StringUtils.defaultString(uniMessage != null ? uniMessage.getTitle() : null));
            message.setText(buildBody(uniMessage));

            return Mono.fromRunnable(() -> mailSender.send(message))
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn("SMTP ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(properties.getHost()) || StringUtils.isBlank(properties.getUsername())
                || StringUtils.isBlank(properties.getPassword()) || StringUtils.isBlank(properties.getFrom())) {
            throw new IllegalStateException("SMTP requires host username password from");
        }
    }

    private static JavaMailSender buildMailSender(Properties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost() == null ? null : properties.getHost().trim());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername() == null ? null : properties.getUsername().trim());
        sender.setPassword(properties.getPassword());
        sender.setDefaultEncoding("UTF-8");

        java.util.Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", "true");
        // align with most providers, avoid old TLS negotiation or unfinished STARTTLS upgrades
        javaMailProperties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        if (properties.isSslEnabled()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            if (StringUtils.isNotBlank(properties.getHost())) {
                javaMailProperties.put("mail.smtp.ssl.trust", properties.getHost().trim());
            }
        } else if (properties.isStartTls()) {
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
            javaMailProperties.put("mail.smtp.starttls.required", "true");
        }
        if (properties.getConnectionTimeout() != null) {
            javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(properties.getConnectionTimeout()));
        }
        if (properties.getReadTimeout() != null) {
            javaMailProperties.put("mail.smtp.timeout", String.valueOf(properties.getReadTimeout()));
        }
        return sender;
    }

    private static String buildBody(UniMessage uniMessage) {
        if (uniMessage == null) {
            return "";
        }
        String title = StringUtils.defaultString(uniMessage.getTitle());
        String content = StringUtils.defaultString(uniMessage.getContent());
        if (StringUtils.isNotBlank(title)) {
            return title + "\n\n" + content;
        }
        return content;
    }

    private List<String> normalizeRecipients(UniAddress uniAddress) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("SMTP recipients required");
        }
        return recipients;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class Properties {

        @NotBlank
        private String host;

        @Min(1)
        @Max(65535)
        private int port;

        @NotBlank
        private String username;

        private String password;

        @NotBlank
        @Email
        private String from;

        private boolean sslEnabled;

        private boolean startTls;

        @Min(0)
        private Integer connectionTimeout;

        @Min(0)
        private Integer readTimeout;

        static Properties fromProperties(Map<String, String> p) {
            if (p == null) {
                p = Map.of();
            }
            int port = parseInt(p.get("port"), 587);
            boolean ssl = parseBool(p.get("sslEnabled"), defaultSslForPort(port));
            boolean startTls = parseBool(firstNonBlank(p.get("starttls"), p.get("startTls")), true);
            Integer conn = parseIntObj(p.get("connectionTimeout"));
            Integer read = parseIntObj(p.get("readTimeout"));
            Properties props = new Properties();
            props.host = trimToNull(p.get("host"));
            props.port = port;
            props.username = trimToNull(p.get("username"));
            props.password = trimPassword(p.get("password"));
            props.from = trimToNull(p.get("from"));
            props.sslEnabled = ssl;
            props.startTls = startTls;
            props.connectionTimeout = conn;
            props.readTimeout = read;
            return props;
        }

        private static String trimToNull(String s) {
            if (s == null) {
                return null;
            }
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }

        private static String trimPassword(String s) {
            if (s == null) {
                return null;
            }
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }

        private static int parseInt(String s, int def) {
            if (StringUtils.isBlank(s)) {
                return def;
            }
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }

        private static Integer parseIntObj(String s) {
            if (StringUtils.isBlank(s)) {
                return null;
            }
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private static boolean parseBool(String s, boolean defaultValue) {
            if (StringUtils.isBlank(s)) {
                return defaultValue;
            }
            return Boolean.parseBoolean(s.trim()) || "1".equals(s.trim()) || "yes".equalsIgnoreCase(s.trim());
        }

        private static boolean defaultSslForPort(int port) {
            return port == 465;
        }

        private static String firstNonBlank(String a, String b) {
            if (StringUtils.isNotBlank(a)) {
                return a;
            }
            return b;
        }
    }
}
