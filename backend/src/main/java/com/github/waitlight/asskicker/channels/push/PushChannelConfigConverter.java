package com.github.waitlight.asskicker.channels.push;

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
public class PushChannelConfigConverter {

    private static final Set<String> APNS_KEYS = Set.of(
            "teamId", "keyId", "bundleId", "p8KeyContent", "p8KeyPath",
            "production", "timeout", "maxRetries", "retryDelay"
    );

    private static final Set<String> FCM_KEYS = Set.of(
            "serviceAccountJson", "projectId", "timeout", "maxRetries", "retryDelay"
    );

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PushChannelConfigConverter(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);
        PushChannelType pushType = parseProtocol(resolveProtocolValue(safe));

        if (pushType == PushChannelType.APNS) {
            Map<String, Object> apnsValues = resolveProtocolValues(safe, APNS_KEYS, "apns", "APNS");
            normalizeDurationValues(apnsValues, "timeout", "retryDelay");
            APNsPushChannelConfig apns = mapToConfig(apnsValues, APNsPushChannelConfig.class, "APNS");
            ensureNonNegativeRetries(apns.getMaxRetries(), "APNS");
            validateApnsBusinessRules(apns);
            validateConfig(apns, "APNS");
            return apns;
        }

        if (pushType == PushChannelType.FCM) {
            Map<String, Object> fcmValues = resolveProtocolValues(safe, FCM_KEYS, "fcm", "FCM");
            normalizeDurationValues(fcmValues, "timeout", "retryDelay");
            FCMPushChannelConfig fcm = mapToConfig(fcmValues, FCMPushChannelConfig.class, "FCM");
            ensureNonNegativeRetries(fcm.getMaxRetries(), "FCM");
            validateConfig(fcm, "FCM");
            return fcm;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported Push protocol type");
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

    private void validateApnsBusinessRules(APNsPushChannelConfig apns) {
        String p8KeyContent = apns.getP8KeyContent();
        String p8KeyPath = apns.getP8KeyPath();
        boolean contentBlank = p8KeyContent == null || p8KeyContent.trim().isEmpty();
        boolean pathBlank = p8KeyPath == null || p8KeyPath.trim().isEmpty();
        if (contentBlank && pathBlank) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APNS requires p8KeyContent or p8KeyPath");
        }
    }

    private void ensureNonNegativeRetries(int maxRetries, String protocol) {
        if (maxRetries < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " maxRetries must be greater than or equal to 0");
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

    private PushChannelType parseProtocol(Object value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push protocol type cannot be blank");
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push protocol type cannot be blank");
        }
        try {
            return PushChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push protocol is not supported: " + normalized);
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