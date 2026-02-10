package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.Sender;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(EmailSenderProperties.class)
public class EmailSenderConfig {

    @Bean
    public JavaMailSender smtpJavaMailSender(EmailSenderProperties properties) {
        EmailSenderProperties.Smtp smtp = properties.getSmtp();

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

    @Bean
    public Sender mailSender(EmailSenderProperties properties,
                             JavaMailSender smtpJavaMailSender,
                             WebClient.Builder webClientBuilder) {
        SmtpEmailSender smtpEmailSender = new SmtpEmailSender(smtpJavaMailSender, properties.getSmtp());
        HttpApiEmailSender httpApiEmailSender = new HttpApiEmailSender(webClientBuilder, properties.getHttpApi());

        if (properties.getProtocol() == EmailProtocolType.HTTP_API) {
            return httpApiEmailSender;
        }
        return smtpEmailSender;
    }
}

