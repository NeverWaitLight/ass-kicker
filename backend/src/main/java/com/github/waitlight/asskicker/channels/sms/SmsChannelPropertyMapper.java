package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class SmsChannelPropertyMapper {

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);
        Object protocolValue = safe.get("type");
        if (protocolValue == null || String.valueOf(protocolValue).trim().isBlank()) {
            protocolValue = safe.get("protocol");
        }
        SmsChannelType smsType = parseProtocol(protocolValue);
        if (smsType == SmsChannelType.ALIYUN) {
            AliyunSmsChannelConfig aliyun = new AliyunSmsChannelConfig();
            Map<String, Object> aliyunNested = readMap(safe.get("aliyun"));
            if (aliyunNested.isEmpty()) {
                aliyunNested = readMap(safe.get("ALIYUN"));
            }
            applyAliyun(aliyun, aliyunNested.isEmpty() ? safe : aliyunNested);
            return aliyun;
        }
        if (smsType == SmsChannelType.TENCENT) {
            TencentSmsChannelConfig tencent = new TencentSmsChannelConfig();
            Map<String, Object> tencentNested = readMap(safe.get("tencent"));
            if (tencentNested.isEmpty()) {
                tencentNested = readMap(safe.get("TENCENT"));
            }
            applyTencent(tencent, tencentNested.isEmpty() ? safe : tencentNested);
            return tencent;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 SMS 协议类型");
    }

    private void applyAliyun(AliyunSmsChannelConfig aliyun, Map<String, Object> values) {
        String accessKeyId = readString(values, "accessKeyId");
        String accessKeySecret = readString(values, "accessKeySecret");
        String signName = readString(values, "signName");
        String templateCode = readString(values, "templateCode");
        if (accessKeyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "阿里云 accessKeyId 不能为空");
        }
        if (accessKeySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "阿里云 accessKeySecret 不能为空");
        }
        if (signName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "阿里云 signName 不能为空");
        }
        if (templateCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "阿里云 templateCode 不能为空");
        }
        aliyun.setAccessKeyId(accessKeyId);
        aliyun.setAccessKeySecret(accessKeySecret);
        aliyun.setSignName(signName);
        aliyun.setTemplateCode(templateCode);
        aliyun.setTemplateParamKey(readString(values, "templateParamKey", aliyun.getTemplateParamKey()));
        aliyun.setRegionId(readString(values, "regionId", aliyun.getRegionId()));
        aliyun.setTimeout(readDuration(values, "timeout", aliyun.getTimeout()));
        aliyun.setMaxRetries(readInt(values, "maxRetries", aliyun.getMaxRetries(), "阿里云最大重试次数非法"));
        aliyun.setRetryDelay(readDuration(values, "retryDelay", aliyun.getRetryDelay()));
    }

    private void applyTencent(TencentSmsChannelConfig tencent, Map<String, Object> values) {
        String secretId = readString(values, "secretId");
        String secretKey = readString(values, "secretKey");
        String sdkAppId = readString(values, "sdkAppId");
        String signName = readString(values, "signName");
        String templateId = readString(values, "templateId");
        if (secretId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "腾讯云 secretId 不能为空");
        }
        if (secretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "腾讯云 secretKey 不能为空");
        }
        if (sdkAppId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "腾讯云 sdkAppId 不能为空");
        }
        if (signName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "腾讯云 signName 不能为空");
        }
        if (templateId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "腾讯云 templateId 不能为空");
        }
        tencent.setSecretId(secretId);
        tencent.setSecretKey(secretKey);
        tencent.setSdkAppId(sdkAppId);
        tencent.setSignName(signName);
        tencent.setTemplateId(templateId);
        tencent.setRegion(readString(values, "region", tencent.getRegion()));
        tencent.setTimeout(readDuration(values, "timeout", tencent.getTimeout()));
        tencent.setMaxRetries(readInt(values, "maxRetries", tencent.getMaxRetries(), "腾讯云最大重试次数非法"));
        tencent.setRetryDelay(readDuration(values, "retryDelay", tencent.getRetryDelay()));
    }

    private SmsChannelType parseProtocol(Object value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS 协议类型不能为空");
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS 协议类型不能为空");
        }
        try {
            return SmsChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMS 协议不支持 " + normalized);
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
