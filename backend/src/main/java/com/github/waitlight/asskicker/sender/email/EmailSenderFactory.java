package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.Sender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class EmailSenderFactory {

    private final WebClient.Builder webClientBuilder;

    public EmailSenderFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Sender create(EmailSenderProperties properties) {
        if (properties.getProtocol() == EmailProtocolType.HTTP_API) {
            return new HttpApiEmailSender(webClientBuilder, properties.getHttpApi());
        }
        JavaMailSender javaMailSender = buildJavaMailSender(properties.getSmtp());
        return new SmtpEmailSender(javaMailSender, properties.getSmtp());
    }

    public JavaMailSender buildJavaMailSender(EmailSenderProperties.Smtp smtp) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setUsername(smtp.getUsername());
        mailSender.setPassword(smtp.getPassword());
        mailSender.setProtocol(smtp.getProtocol());
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = mailSender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", smtp.getProtocol());
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
}
