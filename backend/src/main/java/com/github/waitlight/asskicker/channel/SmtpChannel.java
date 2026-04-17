package com.github.waitlight.asskicker.channel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.dto.UniAddress;
import com.github.waitlight.asskicker.dto.UniMessage;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ProviderType;

import jakarta.mail.Authenticator;
import jakarta.validation.constraints.NotBlank;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@ChannelImpl(providerType = ProviderType.SMTP, propertyClass = SmtpChannel.Properties.class)
public class SmtpChannel extends Channel {

    private final Properties properties;

    public SmtpChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.properties = Properties.fromProperties(provider.getProperties());
    }

    @Override
    protected Mono<String> doSend(UniMessage uniMessage, UniAddress uniAddress) {
        return Mono.defer(() -> {
            List<String> recipients = normalizeRecipients(uniAddress, "SMTP");
            validateSpec();

            String subject = uniMessage != null && StringUtils.isNotBlank(uniMessage.getTitle())
                    ? uniMessage.getTitle()
                    : "";
            String bodyText = buildBody(uniMessage);

            return Mono.fromCallable(() -> {
                java.util.Properties props = buildMailProperties();
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.username().trim(), properties.password());
                    }
                });
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(properties.from().trim()));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(String.join(",", recipients)));
                message.setSubject(subject, "UTF-8");
                message.setText(bodyText, "UTF-8");
                Transport.send(message);
                return "SMTP ok " + recipients.size() + " recipient(s)";
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private void validateSpec() {
        if (StringUtils.isBlank(properties.host()) || StringUtils.isBlank(properties.username())
                || StringUtils.isBlank(properties.password()) || StringUtils.isBlank(properties.from())) {
            throw new IllegalStateException("SMTP requires host username password from");
        }
    }

    private java.util.Properties buildMailProperties() {
        java.util.Properties props = new java.util.Properties();
        String host = properties.host().trim();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(properties.port()));
        props.put("mail.smtp.auth", "true");
        // 与多数现网 SMTP 对齐，避免协商到过旧 TLS 或 STARTTLS 未升级导致认证异常
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        if (properties.sslEnabled()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
        } else if (properties.startTls()) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        if (properties.connectionTimeout() != null) {
            props.put("mail.smtp.connectiontimeout", String.valueOf(properties.connectionTimeout()));
        }
        if (properties.readTimeout() != null) {
            props.put("mail.smtp.timeout", String.valueOf(properties.readTimeout()));
        }
        return props;
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

    private List<String> normalizeRecipients(UniAddress uniAddress, String providerName) {
        List<String> recipients = uniAddress == null || uniAddress.getRecipients() == null
                ? List.of()
                : uniAddress.getRecipients().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(providerName + " recipients required");
        }
        return recipients;
    }

    record Properties(
            @NotBlank(message = "host 不能为空") String host,
            int port,
            @NotBlank(message = "username 不能为空") String username,
            String password,
            @NotBlank(message = "from 不能为空") String from,
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

        /** 去掉首尾空白，避免复制粘贴带入空格导致服务商返回 526 等认证失败 */
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
         * 465 为 SMTPS 隐式 TLS 常用端口，未显式配置 sslEnabled 时应默认开启，否则易出现 bad greeting EOF
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
