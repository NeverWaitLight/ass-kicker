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
    void shouldPassValidationForValidRange() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setMinSleepMs(60);
        properties.setMaxSleepMs(120);

        Set<ConstraintViolation<ChannelDebugProperties>> violations = validator.validate(properties);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWhenMaxLessThanMin() {
        ChannelDebugProperties properties = new ChannelDebugProperties();
        properties.setMinSleepMs(120);
        properties.setMaxSleepMs(60);

        Set<ConstraintViolation<ChannelDebugProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty());
    }
}
