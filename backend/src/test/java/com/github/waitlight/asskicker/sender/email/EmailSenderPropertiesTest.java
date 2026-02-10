package com.github.waitlight.asskicker.sender.email;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSenderPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    WebClientAutoConfiguration.class))
            .withUserConfiguration(EmailSenderConfig.class);

    @Test
    void shouldBindSmtpMailProperties() {
        contextRunner
                .withPropertyValues(
                        "app.sender.mail.protocol=SMTP",
                        "app.sender.mail.smtp.host=smtp.example.com",
                        "app.sender.mail.smtp.port=465",
                        "app.sender.mail.smtp.username=tester@example.com",
                        "app.sender.mail.smtp.password=secret",
                        "app.sender.mail.smtp.protocol=smtp",
                        "app.sender.mail.smtp.ssl-enabled=true",
                        "app.sender.mail.smtp.from=notify@example.com",
                        "app.sender.mail.smtp.connection-timeout=5s",
                        "app.sender.mail.smtp.read-timeout=10s",
                        "app.sender.mail.smtp.max-retries=3",
                        "app.sender.mail.smtp.retry-delay=1s",
                        "app.sender.mail.http-api.base-url=https://api.example.com",
                        "app.sender.mail.http-api.path=/mail/send",
                        "app.sender.mail.http-api.api-key=test-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderProperties.class);
                    EmailSenderProperties properties = context.getBean(EmailSenderProperties.class);
                    assertThat(properties.getProtocol()).isEqualTo(EmailProtocolType.SMTP);

                    EmailSenderProperties.Smtp smtp = properties.getSmtp();
                    assertThat(smtp.getHost()).isEqualTo("smtp.example.com");
                    assertThat(smtp.getPort()).isEqualTo(465);
                    assertThat(smtp.getUsername()).isEqualTo("tester@example.com");
                    assertThat(smtp.getPassword()).isEqualTo("secret");
                    assertThat(smtp.getProtocol()).isEqualTo("smtp");
                    assertThat(smtp.isSslEnabled()).isTrue();
                    assertThat(smtp.getFrom()).isEqualTo("notify@example.com");
                    assertThat(smtp.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(smtp.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(smtp.getMaxRetries()).isEqualTo(3);
                    assertThat(smtp.getRetryDelay()).isEqualTo(Duration.ofSeconds(1));
                });
    }

    @Test
    void shouldBindHttpApiMailProperties() {
        contextRunner
                .withPropertyValues(
                        "app.sender.mail.protocol=HTTP_API",
                        "app.sender.mail.http-api.base-url=https://api.example.com",
                        "app.sender.mail.http-api.path=/api/mail/send",
                        "app.sender.mail.http-api.api-key-header=X-API-Key",
                        "app.sender.mail.http-api.api-key=test-api-key",
                        "app.sender.mail.http-api.from=notify@example.com",
                        "app.sender.mail.http-api.timeout=5s",
                        "app.sender.mail.http-api.max-retries=3",
                        "app.sender.mail.http-api.retry-delay=1s",
                        "app.sender.mail.smtp.host=smtp.example.com",
                        "app.sender.mail.smtp.port=465",
                        "app.sender.mail.smtp.username=tester@example.com",
                        "app.sender.mail.smtp.password=secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSenderProperties.class);
                    EmailSenderProperties properties = context.getBean(EmailSenderProperties.class);
                    assertThat(properties.getProtocol()).isEqualTo(EmailProtocolType.HTTP_API);

                    EmailSenderProperties.HttpApi httpApi = properties.getHttpApi();
                    assertThat(httpApi.getBaseUrl()).isEqualTo("https://api.example.com");
                    assertThat(httpApi.getPath()).isEqualTo("/api/mail/send");
                    assertThat(httpApi.getApiKeyHeader()).isEqualTo("X-API-Key");
                    assertThat(httpApi.getApiKey()).isEqualTo("test-api-key");
                    assertThat(httpApi.getFrom()).isEqualTo("notify@example.com");
                    assertThat(httpApi.getTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(httpApi.getMaxRetries()).isEqualTo(3);
                    assertThat(httpApi.getRetryDelay()).isEqualTo(Duration.ofSeconds(1));
                });
    }
}
