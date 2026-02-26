package com.github.waitlight.asskicker.channels.im;

import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class IMChannelPropertyMapper {

    private static final Set<String> DINGTALK_KEYS = Set.of(
            "webhookUrl", "accessToken", "secret",
            "timeout", "maxRetries", "retryDelay"
    );

    private static final Set<String> WECHAT_WORK_KEYS = Set.of(
            "webhookUrl", "timeout", "maxRetries", "retryDelay"
    );

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);

        // 支持 properties.type 和 properties.protocol 两种方式读取协议
        Object protocolValue = safe.get("type");
        if (protocolValue == null || String.valueOf(protocolValue).trim().isBlank()) {
            protocolValue = safe.get("protocol");
        }
        IMChannelType senderType = parseProtocol(protocolValue);
        if (senderType == IMChannelType.DINGTALK) {
            DingTalkIMChannelConfig dingTalk = new DingTalkIMChannelConfig();
            Map<String, Object> dingTalkNested = readMap(safe.get("dingTalk"));
            if (dingTalkNested.isEmpty()) {
                dingTalkNested = readMap(safe.get("dingtalk"));
            }
            applyDingTalk(dingTalk, dingTalkNested);
            return dingTalk;
        }
        if (senderType == IMChannelType.WECHAT_WORK) {
            WechatWorkIMChannelConfig wechatWork = new WechatWorkIMChannelConfig();
            Map<String, Object> wechatWorkNested = readMap(safe.get("wechatWork"));
            if (wechatWorkNested.isEmpty()) {
                wechatWorkNested = readMap(safe.get("wechat_work"));
            }
            applyWechatWork(wechatWork, wechatWorkNested);
            return wechatWork;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 IM 协议类型");
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

    private void applyDingTalk(DingTalkIMChannelConfig dingTalk, Map<String, Object> values) {
        String webhookUrl = readString(values, "webhookUrl");

        if (webhookUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "钉钉 Webhook 地址不能为空");
        }

        // 从 webhookUrl 中提取 access_token
        String accessToken = extractAccessToken(webhookUrl);
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "钉钉 Webhook URL 中必须包含 access_token 参数");
        }

        dingTalk.setWebhookUrl(webhookUrl);
        dingTalk.setAccessToken(accessToken);
        dingTalk.setSecret(readString(values, "secret", dingTalk.getSecret()));
        dingTalk.setTimeout(readDuration(values, "timeout", dingTalk.getTimeout()));
        dingTalk.setMaxRetries(readInt(values, "maxRetries", dingTalk.getMaxRetries(), "钉钉最大重试次数非法"));
        dingTalk.setRetryDelay(readDuration(values, "retryDelay", dingTalk.getRetryDelay()));
    }

    private void applyWechatWork(WechatWorkIMChannelConfig wechatWork, Map<String, Object> values) {
        String webhookUrl = readString(values, "webhookUrl");
        if (webhookUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "企业微信 Webhook 地址不能为空");
        }
        wechatWork.setWebhookUrl(webhookUrl);
        wechatWork.setTimeout(readDuration(values, "timeout", wechatWork.getTimeout()));
        wechatWork.setMaxRetries(readInt(values, "maxRetries", wechatWork.getMaxRetries(), "企业微信最大重试次数非法"));
        wechatWork.setRetryDelay(readDuration(values, "retryDelay", wechatWork.getRetryDelay()));
    }

    /**
     * 从 Webhook URL 中提取 access_token
     */
    private String extractAccessToken(String webhookUrl) {
        if (!webhookUrl.contains("access_token=")) {
            return "";
        }
        String[] parts = webhookUrl.split("\\?");
        if (parts.length < 2) {
            return "";
        }
        String[] queryParams = parts[1].split("&");
        for (String param : queryParams) {
            if (param.startsWith("access_token=")) {
                return param.substring("access_token=".length());
            }
        }
        return "";
    }

    private IMChannelType parseProtocol(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return IMChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IM 协议不支持 " + normalized);
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
            if (parsed <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
            }
            return parsed;
        }
        try {
            int parsed = Integer.parseInt(String.valueOf(value).trim());
            if (parsed <= 0) {
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
