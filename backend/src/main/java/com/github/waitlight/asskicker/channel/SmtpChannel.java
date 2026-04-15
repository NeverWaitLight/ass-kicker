package com.github.waitlight.asskicker.channel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
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

@ChannelImpl(providerType = ProviderType.SMTP, propertyClass = SmtpChannel.Spec.class)
public class SmtpChannel extends Channel {

    private final Spec spec;

    public SmtpChannel(ChannelEntity provider, WebClient webClient, ObjectMapper objectMapper) {
        super(provider, webClient, objectMapper);
        this.spec = Spec.fromProperties(provider.getProperties());
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
                Properties props = buildMailProperties();
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(spec.username().trim(), spec.password());
                    }
                });
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(spec.from().trim()));
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
        if (StringUtils.isBlank(spec.host()) || StringUtils.isBlank(spec.username())
                || spec.password() == null || StringUtils.isBlank(spec.from())) {
            throw new IllegalStateException("SMTP requires host username password from");
        }
    }

    private Properties buildMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.host", spec.host().trim());
        props.put("mail.smtp.port", String.valueOf(spec.port()));
        props.put("mail.smtp.auth", "true");
        if (spec.sslEnabled()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", spec.host().trim());
        } else if (spec.startTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        if (spec.connectionTimeout() != null) {
            props.put("mail.smtp.connectiontimeout", String.valueOf(spec.connectionTimeout()));
        }
        if (spec.readTimeout() != null) {
            props.put("mail.smtp.timeout", String.valueOf(spec.readTimeout()));
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

    record Spec(
            @NotBlank(message = "host 不能为空") String host,
            int port,
            @NotBlank(message = "username 不能为空") String username,
            String password,
            @NotBlank(message = "from 不能为空") String from,
            boolean sslEnabled,
            boolean startTls,
            Integer connectionTimeout,
            Integer readTimeout) {

        static Spec fromProperties(Map<String, String> p) {
            if (p == null) {
                p = Map.of();
            }
            int port = parseInt(p.get("port"), 587);
            boolean ssl = parseBool(p.get("sslEnabled"), false);
            boolean startTls = parseBool(p.get("starttls"), true);
            Integer conn = parseIntObj(p.get("connectionTimeout"));
            Integer read = parseIntObj(p.get("readTimeout"));
            return new Spec(
                    p.get("host"),
                    port,
                    p.get("username"),
                    p.get("password"),
                    p.get("from"),
                    ssl,
                    startTls,
                    conn,
                    read);
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
    }
}
