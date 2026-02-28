package com.github.waitlight.asskicker.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelDebugPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldPassValidationForValidSleepMs() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setSleepMs(100);

        Set<ConstraintViolation<ChannelDebugProperties>> violations = validator.validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWhenSleepMsIsNegative() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setSleepMs(-1);

        Set<ConstraintViolation<ChannelDebugProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
}
