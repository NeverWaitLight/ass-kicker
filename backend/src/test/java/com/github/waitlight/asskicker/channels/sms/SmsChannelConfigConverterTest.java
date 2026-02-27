package com.github.waitlight.asskicker.channels.sms;

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

class SmsChannelConfigConverterTest {

    private final SmsChannelConfigConverter converter =
            new SmsChannelConfigConverter(JsonMapper.builder().findAndAddModules().build(), validator());

    @Test
    void shouldConvertAliyunFromNestedValues() {
        Map<String, Object> properties = Map.of(
                "type", "ALIYUN",
                "aliyun", Map.of(
                        "accessKeyId", "id",
                        "accessKeySecret", "secret",
                        "signName", "sign",
                        "templateCode", "t1",
                        "timeout", "PT7S",
                        "maxRetries", 1,
                        "retryDelay", "PT2S"
                )
        );

        AliyunSmsChannelConfig config = assertInstanceOf(
                AliyunSmsChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("id", config.getAccessKeyId());
        assertEquals("sign", config.getSignName());
        assertEquals(Duration.ofSeconds(7), config.getTimeout());
    }

    @Test
    void shouldConvertTencentFromRootValues() {
        Map<String, Object> properties = Map.of(
                "protocol", "TENCENT",
                "secretId", "id",
                "secretKey", "key",
                "sdkAppId", "app",
                "signName", "sign",
                "templateId", "tpl",
                "maxRetries", 0
        );

        TencentSmsChannelConfig config = assertInstanceOf(
                TencentSmsChannelConfig.class,
                converter.fromProperties(properties)
        );

        assertEquals("id", config.getSecretId());
        assertEquals("tpl", config.getTemplateId());
        assertEquals(0, config.getMaxRetries());
    }

    @Test
    void shouldFailWhenTencentRetriesIsNegative() {
        Map<String, Object> properties = Map.of(
                "type", "TENCENT",
                "secretId", "id",
                "secretKey", "key",
                "sdkAppId", "app",
                "signName", "sign",
                "templateId", "tpl",
                "maxRetries", -1
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> converter.fromProperties(properties));

        assertEquals("TENCENT maxRetries must be greater than or equal to 0", ex.getReason());
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}