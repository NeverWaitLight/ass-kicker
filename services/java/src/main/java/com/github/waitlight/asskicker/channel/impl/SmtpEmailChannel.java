package com.github.waitlight.asskicker.channel.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;

import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ChannelImpl(providerType = ProviderType.SMTP, propertyClass = SmtpEmailChannel.Properties.class)
public class SmtpEmailChannel extends Channel {

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
            message.setFrom(properties.from().trim());
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject(StringUtils.defaultString(uniMessage != null ? uniMessage.getTitle() : null));
            message.setText(buildBody(uniMessage));

            return Mono.fromRunnable(() -> mailSender.send(message))
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn("SMTP ok " + recipients.size() + " recipient(s)");
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(properties.host()) || StringUtils.isBlank(properties.username())
                || StringUtils.isBlank(properties.password()) || StringUtils.isBlank(properties.from())) {
            throw new IllegalStateException("SMTP requires host username password from");
        }
    }

    private static JavaMailSender buildMailSender(Properties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host() == null ? null : properties.host().trim());
        sender.setPort(properties.port());
        sender.setUsername(properties.username() == null ? null : properties.username().trim());
        sender.setPassword(properties.password());
        sender.setDefaultEncoding("UTF-8");

        java.util.Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", "true");
        // align with most providers, avoid old TLS negotiation or unfinished STARTTLS upgrades
        javaMailProperties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        if (properties.sslEnabled()) {
            javaMailProperties.put("mail.smtp.ssl.enable", "true");
            if (StringUtils.isNotBlank(properties.host())) {
                javaMailProperties.put("mail.smtp.ssl.trust", properties.host().trim());
            }
        } else if (properties.startTls()) {
            javaMailProperties.put("mail.smtp.starttls.enable", "true");
            javaMailProperties.put("mail.smtp.starttls.required", "true");
        }
        if (properties.connectionTimeout() != null) {
            javaMailProperties.put("mail.smtp.connectiontimeout", String.valueOf(properties.connectionTimeout()));
        }
        if (properties.readTimeout() != null) {
            javaMailProperties.put("mail.smtp.timeout", String.valueOf(properties.readTimeout()));
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

    record Properties(
            @NotBlank String host,
            int port,
            @NotBlank String username,
            String password,
            @NotBlank String from,
            boolean sslEnabled,
            boolean startTls,
            Integer connectionTimeout,
            Integer readTimeout) {

        static Properties fromProperties(Map<String, String> p) {
            if (p == null) {
                p = Map.of();
            }
            int port = parseInt(p.get("port"), 587);
            boolean ssl = parseBool(p.get("sslEnabled"), defaultSslForPort(port));
            boolean startTls = parseBool(firstNonBlank(p.get("starttls"), p.get("startTls")), true);
            Integer conn = parseIntObj(p.get("connectionTimeout"));
            Integer read = parseIntObj(p.get("readTimeout"));
            return new Properties(
                    trimToNull(p.get("host")),
                    port,
                    trimToNull(p.get("username")),
                    trimPassword(p.get("password")),
                    trimToNull(p.get("from")),
                    ssl,
                    startTls,
                    conn,
                    read);
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

        /**
         * 465 is the implicit-TLS SMTPS port; default to SSL on when sslEnabled is unset to avoid bad-greeting EOF
         */
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
