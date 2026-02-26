package com.github.waitlight.asskicker.sender.im;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DingTalkIMSenderConfigTest {

    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    @Test
    void shouldCreateValidConfigWithRequiredFields() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenWebhookUrlIsBlank() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("");

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting("message").containsAnyOf("不能为空", "must not be blank");
    }

    @Test
    void shouldFailValidationWhenWebhookUrlIsNull() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl(null);

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting("message").containsAnyOf("不能为空", "must not be blank");
    }

    @Test
    void shouldAcceptOptionalSecret() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        config.setSecret("SEC123456");

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldUseDefaultTimeout() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");

        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void shouldUseDefaultMaxRetries() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");

        assertThat(config.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void shouldUseDefaultRetryDelay() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");

        assertThat(config.getRetryDelay()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void shouldAllowCustomTimeout() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        config.setTimeout(Duration.ofSeconds(10));

        assertThat(config.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void shouldAllowZeroMaxRetries() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        config.setMaxRetries(0);

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isEmpty();
        assertThat(config.getMaxRetries()).isEqualTo(0);
    }

    @Test
    void shouldFailValidationWhenMaxRetriesIsNegative() {
        DingTalkIMSenderConfig config = new DingTalkIMSenderConfig();
        config.setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test");
        config.setMaxRetries(-1);

        Set<ConstraintViolation<DingTalkIMSenderConfig>> violations = validator.validate(config);

        assertThat(violations).isNotEmpty();
    }
}
