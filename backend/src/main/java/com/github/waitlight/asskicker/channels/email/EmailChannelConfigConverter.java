package com.github.waitlight.asskicker.channels.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EmailChannelConfigConverter {

    private static final Set<String> SMTP_KEYS = Set.of(
            "host", "port", "username", "password",
            "sslEnabled", "from", "connectionTimeout", "readTimeout",
            "maxRetries", "retryDelay"
    );

    private static final Set<String> HTTP_API_KEYS = Set.of(
            "baseUrl", "path", "apiKeyHeader", "apiKey",
            "from", "timeout", "maxRetries", "retryDelay"
    );

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public EmailChannelConfigConverter(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);
        EmailChannelType protocol = parseProtocol(resolveProtocolValue(safe));

        if (protocol == EmailChannelType.HTTP) {
            Map<String, Object> httpValues = resolveProtocolValues(safe, HTTP_API_KEYS, "httpApi", "httpAPI", "http");
            normalizeDurationValues(httpValues, "timeout", "retryDelay");
            HttpEmailChannelConfig httpConfig = mapToConfig(httpValues, HttpEmailChannelConfig.class, "HTTP");
            ensurePositiveRetries(httpConfig.getMaxRetries(), "HTTP");
            validateConfig(httpConfig, "HTTP");
            return httpConfig;
        }

        Map<String, Object> smtpValues = resolveProtocolValues(safe, SMTP_KEYS, "smtp", "SMTP");
        normalizeDurationValues(smtpValues, "connectionTimeout", "readTimeout", "retryDelay");
        SmtpEmailChannelConfig smtpConfig = mapToConfig(smtpValues, SmtpEmailChannelConfig.class, "SMTP");
        ensurePositiveRetries(smtpConfig.getMaxRetries(), "SMTP");
        validateConfig(smtpConfig, "SMTP");
        return smtpConfig;
    }

    private Object resolveProtocolValue(Map<String, Object> safe) {
        Object protocolValue = safe.get("type");
        if (protocolValue == null || String.valueOf(protocolValue).trim().isBlank()) {
            protocolValue = safe.get("protocol");
        }
        return protocolValue;
    }

    private Map<String, Object> resolveProtocolValues(Map<String, Object> root,
                                                      Set<String> allowedKeys,
                                                      String... nestedAliases) {
        Map<String, Object> nested = new LinkedHashMap<>();
        for (String alias : nestedAliases) {
            nested = readMap(root.get(alias));
            if (!nested.isEmpty()) {
                break;
            }
        }
        return mergeProtocolValues(nested, root, allowedKeys);
    }

    private Map<String, Object> mergeProtocolValues(Map<String, Object> nested,
                                                    Map<String, Object> root,
                                                    Set<String> allowedKeys) {
        Map<String, Object> result = new LinkedHashMap<>(nested);
        for (String key : allowedKeys) {
            if ((!result.containsKey(key) || result.get(key) == null) && root.containsKey(key)) {
                result.put(key, root.get(key));
            }
        }
        return result;
    }

    private void normalizeDurationValues(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            if (!values.containsKey(key)) {
                continue;
            }
            Duration parsed = parseDuration(values.get(key));
            if (parsed == null) {
                values.remove(key);
            } else {
                values.put(key, parsed);
            }
        }
    }

    private Duration parseDuration(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            if (text.toLowerCase(Locale.ROOT).startsWith("pt")) {
                return Duration.parse(text);
            }
            return Duration.ofMillis(Long.parseLong(text));
        } catch (Exception ex) {
            return null;
        }
    }

    private <T> T mapToConfig(Map<String, Object> values, Class<T> targetType, String protocol) {
        try {
            return objectMapper.convertValue(values, targetType);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " config format is invalid", ex);
        }
    }

    private <T> void validateConfig(T config, String protocol) {
        Set<ConstraintViolation<T>> violations = validator.validate(config);
        if (violations.isEmpty()) {
            return;
        }
        String message = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " config validation failed: " + message);
    }

    private void ensurePositiveRetries(int maxRetries, String protocol) {
        if (maxRetries <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " maxRetries must be greater than 0");
        }
    }

    private EmailChannelType parseProtocol(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return EmailChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email protocol is not supported: " + normalized);
        }
    }

    private Map<String, Object> normalizeProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(properties);
    }

    private Map<String, Object> readMap(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }
}