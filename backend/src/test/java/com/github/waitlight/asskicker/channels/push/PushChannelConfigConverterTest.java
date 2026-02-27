package com.github.waitlight.asskicker.channels.push;

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

class PushChannelConfigConverterTest {

    private final PushChannelConfigConverter converter =
            new PushChannelConfigConverter(JsonMapper.builder().findAndAddModules().build(), validator());

    @Test
    void shouldConvertApnsFromNestedValues() {
        Map<String, Object> properties = Map.of(
                "type", "APNS",
                "apns", Map.of(
                        "teamId", "team",
                        "keyId", "key",
                        "bundleId", "bundle",
                        "p8KeyContent", "content",
                        "timeout", "PT8S",
                        "maxRetries", 2,
                        "retryDelay", "PT1S"
                )
        );

        APNsPushChannelConfig config = assertInstanceOf(
                APNsPushChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("team", config.getTeamId());
        assertEquals("content", config.getP8KeyContent());
        assertEquals(Duration.ofSeconds(8), config.getTimeout());
    }

    @Test
    void shouldFailWhenApnsP8Missing() {
        Map<String, Object> properties = Map.of(
                "protocol", "APNS",
                "teamId", "team",
                "keyId", "key",
                "bundleId", "bundle"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> converter.fromProperties(properties));

        assertEquals("APNS requires p8KeyContent or p8KeyPath", ex.getReason());
    }

    @Test
    void shouldConvertFcmFromRootValues() {
        Map<String, Object> properties = Map.of(
                "type", "FCM",
                "serviceAccountJson", "{\"type\":\"service_account\"}",
                "projectId", "p",
                "maxRetries", 0
        );

        FCMPushChannelConfig config = assertInstanceOf(
                FCMPushChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("p", config.getProjectId());
        assertEquals(0, config.getMaxRetries());
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}