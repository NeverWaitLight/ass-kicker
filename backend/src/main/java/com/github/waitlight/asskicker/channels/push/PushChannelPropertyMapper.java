package com.github.waitlight.asskicker.channels.push;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class PushChannelPropertyMapper {

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);
        Object protocolValue = safe.get("type");
        if (protocolValue == null || String.valueOf(protocolValue).trim().isBlank()) {
            protocolValue = safe.get("protocol");
        }
        PushChannelType pushType = parseProtocol(protocolValue);
        if (pushType == PushChannelType.APNS) {
            APNsPushChannelConfig apns = new APNsPushChannelConfig();
            Map<String, Object> apnsNested = readMap(safe.get("apns"));
            if (apnsNested.isEmpty()) {
                apnsNested = readMap(safe.get("APNS"));
            }
            applyApns(apns, apnsNested.isEmpty() ? safe : apnsNested);
            return apns;
        }
        if (pushType == PushChannelType.FCM) {
            FCMPushChannelConfig fcm = new FCMPushChannelConfig();
            Map<String, Object> fcmNested = readMap(safe.get("fcm"));
            if (fcmNested.isEmpty()) {
                fcmNested = readMap(safe.get("FCM"));
            }
            applyFcm(fcm, fcmNested.isEmpty() ? safe : fcmNested);
            return fcm;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 Push 协议类型");
    }

    private void applyApns(APNsPushChannelConfig apns, Map<String, Object> values) {
        String teamId = readString(values, "teamId");
        String keyId = readString(values, "keyId");
        String bundleId = readString(values, "bundleId");
        if (teamId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APNs teamId 不能为空");
        }
        if (keyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APNs keyId 不能为空");
        }
        if (bundleId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APNs bundleId 不能为空");
        }
        String p8KeyContent = readString(values, "p8KeyContent");
        String p8KeyPath = readString(values, "p8KeyPath");
        if (p8KeyContent.isBlank() && p8KeyPath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APNs 需配置 p8KeyContent 或 p8KeyPath");
        }
        apns.setTeamId(teamId);
        apns.setKeyId(keyId);
        apns.setBundleId(bundleId);
        apns.setP8KeyContent(p8KeyContent.isBlank() ? null : p8KeyContent);
        apns.setP8KeyPath(p8KeyPath.isBlank() ? null : p8KeyPath);
        apns.setProduction(readBoolean(values, "production", true));
        apns.setTimeout(readDuration(values, "timeout", apns.getTimeout()));
        apns.setMaxRetries(readInt(values, "maxRetries", apns.getMaxRetries(), "APNs 最大重试次数非法"));
        apns.setRetryDelay(readDuration(values, "retryDelay", apns.getRetryDelay()));
    }

    private void applyFcm(FCMPushChannelConfig fcm, Map<String, Object> values) {
        String serviceAccountJson = readString(values, "serviceAccountJson");
        if (serviceAccountJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FCM serviceAccountJson 不能为空");
        }
        fcm.setServiceAccountJson(serviceAccountJson);
        fcm.setProjectId(readString(values, "projectId", fcm.getProjectId()));
        fcm.setTimeout(readDuration(values, "timeout", fcm.getTimeout()));
        fcm.setMaxRetries(readInt(values, "maxRetries", fcm.getMaxRetries(), "FCM 最大重试次数非法"));
        fcm.setRetryDelay(readDuration(values, "retryDelay", fcm.getRetryDelay()));
    }

    private PushChannelType parseProtocol(Object value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push 协议类型不能为空");
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push 协议类型不能为空");
        }
        try {
            return PushChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push 协议不支持 " + normalized);
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

    private String readString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String readString(Map<String, Object> values, String key, String fallback) {
        String value = readString(values, key);
        if (!value.isBlank()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }

    private boolean readBoolean(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return fallback;
        }
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    private int readInt(Map<String, Object> values, String key, int fallback, String errorMessage) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).trim().isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            int parsed = number.intValue();
            if (parsed < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
            }
            return parsed;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value).trim());
            if (parsed < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private Duration readDuration(Map<String, Object> values, String key, Duration fallback) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).trim().isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        String text = String.valueOf(value).trim();
        try {
            if (text.toLowerCase(Locale.ROOT).startsWith("pt")) {
                return Duration.parse(text);
            }
            long millis = Long.parseLong(text);
            return Duration.ofMillis(millis);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
