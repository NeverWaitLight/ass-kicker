package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpEmailSenderTest {

    @Test
    void shouldBuildJavaMailSenderFromConfig() throws Exception {
        SmtpEmailSenderConfig config = createConfig();
        SmtpEmailSender sender = new SmtpEmailSender(config);

        Field field = SmtpEmailSender.class.getDeclaredField("mailSender");
        field.setAccessible(true);
        JavaMailSender mailSender = (JavaMailSender) field.get(sender);

        assertThat(mailSender).isInstanceOf(JavaMailSenderImpl.class);

        JavaMailSenderImpl impl = (JavaMailSenderImpl) mailSender;
        assertThat(impl.getHost()).isEqualTo("smtp.example.com");
        assertThat(impl.getPort()).isEqualTo(465);
        assertThat(impl.getUsername()).isEqualTo("test@example.com");
        assertThat(impl.getPassword()).isEqualTo("password");
        assertThat(impl.getProtocol()).isEqualTo("smtp");
        assertThat(impl.getDefaultEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

        Properties props = impl.getJavaMailProperties();
        assertThat(props.get("mail.transport.protocol")).isEqualTo("smtp");
        assertThat(props.get("mail.smtp.auth")).isEqualTo("true");
        assertThat(props.get("mail.smtp.connectiontimeout")).isEqualTo(String.valueOf(Duration.ofSeconds(5).toMillis()));
        assertThat(props.get("mail.smtp.timeout")).isEqualTo(String.valueOf(Duration.ofSeconds(10).toMillis()));
        assertThat(props.get("mail.smtp.writetimeout")).isEqualTo(String.valueOf(Duration.ofSeconds(10).toMillis()));
        assertThat(props.get("mail.smtp.ssl.enable")).isEqualTo("true");
        assertThat(props.get("mail.smtp.ssl.trust")).isEqualTo("smtp.example.com");
    }

    @Test
    void shouldReturnFailureWhenRequestIsNull() {
        SmtpEmailSender sender = new SmtpEmailSender(createConfig());

        MessageResponse response = sender.send(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void shouldResolveFromWithFallback() throws Exception {
        SmtpEmailSenderConfig config = createConfig();
        SmtpEmailSender sender = new SmtpEmailSender(config);
        Method method = SmtpEmailSender.class.getDeclaredMethod("resolveFrom");
        method.setAccessible(true);

        Object from = method.invoke(sender);
        assertThat(from).isEqualTo("notify@example.com");

        config.setFrom("   ");
        SmtpEmailSender fallbackSender = new SmtpEmailSender(config);
        Object fallback = method.invoke(fallbackSender);
        assertThat(fallback).isEqualTo("test@example.com");
    }

    private SmtpEmailSenderConfig createConfig() {
        SmtpEmailSenderConfig config = new SmtpEmailSenderConfig();
        config.setHost("smtp.example.com");
        config.setPort(465);
        config.setUsername("test@example.com");
        config.setPassword("password");
        config.setSslEnabled(true);
        config.setFrom("notify@example.com");
        config.setConnectionTimeout(Duration.ofSeconds(5));
        config.setReadTimeout(Duration.ofSeconds(10));
        config.setMaxRetries(3);
        config.setRetryDelay(Duration.ofMillis(100));
        return config;
    }
}
