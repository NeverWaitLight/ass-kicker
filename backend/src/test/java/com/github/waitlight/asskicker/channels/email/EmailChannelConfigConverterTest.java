package com.github.waitlight.asskicker.channels.email;

import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailChannelConfigConverterTest {

    private final EmailChannelConfigConverter converter =
            new EmailChannelConfigConverter(JsonMapper.builder().findAndAddModules().build(), validator());

    @Test
    void shouldDefaultToSmtpWhenProtocolMissing() {
        Map<String, Object> properties = Map.of(
                "host", "smtp.example.com",
                "username", "user",
                "password", "pwd",
                "port", 587,
                "maxRetries", 2,
                "connectionTimeout", "PT3S",
                "readTimeout", "PT4S",
                "retryDelay", "PT1S"
        );

        SmtpEmailChannelConfig config = assertInstanceOf(
                SmtpEmailChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("smtp.example.com", config.getHost());
        assertEquals("user", config.getUsername());
        assertEquals("pwd", config.getPassword());
        assertEquals(587, config.getPort());
        assertEquals(2, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(3), config.getConnectionTimeout());
    }

    @Test
    void shouldConvertHttpFromNestedValues() {
        Map<String, Object> properties = Map.of(
                "protocol", "HTTP",
                "httpApi", Map.of(
                        "baseUrl", "https://api.example.com",
                        "path", "/mail/send",
                        "apiKeyHeader", "X-Key",
                        "apiKey", "k",
                        "timeout", "PT5S",
                        "maxRetries", 1,
                        "retryDelay", "PT2S"
                )
        );

        HttpEmailChannelConfig config = assertInstanceOf(
                HttpEmailChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("https://api.example.com", config.getBaseUrl());
        assertEquals("/mail/send", config.getPath());
        assertEquals(1, config.getMaxRetries());
        assertEquals(Duration.ofSeconds(5), config.getTimeout());
    }

    @Test
    void shouldFailWhenSmtpRetriesIsZero() {
        Map<String, Object> properties = Map.of(
                "protocol", "SMTP",
                "host", "smtp.example.com",
                "username", "user",
                "password", "pwd",
                "maxRetries", 0
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> converter.fromProperties(properties));

        assertEquals("SMTP maxRetries must be greater than 0", ex.getReason());
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}