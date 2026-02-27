package com.github.waitlight.asskicker.channels.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IMChannelConfigConverter {

    private static final Set<String> DINGTALK_KEYS = Set.of(
            "webhookUrl", "accessToken", "secret",
            "timeout", "maxRetries", "retryDelay"
    );

    private static final Set<String> WECHAT_WORK_KEYS = Set.of(
            "webhookUrl", "timeout", "maxRetries", "retryDelay"
    );

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public IMChannelConfigConverter(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public ChannelConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);
        IMChannelType senderType = parseProtocol(resolveProtocolValue(safe));

        if (senderType == IMChannelType.DINGTALK) {
            Map<String, Object> dingTalkValues = resolveProtocolValues(safe, DINGTALK_KEYS, "dingTalk", "dingtalk");
            DingTalkIMChannelConfig dingTalk = mapToConfig(dingTalkValues, DingTalkIMChannelConfig.class, "DINGTALK");
            applyDingTalkDerivedFields(dingTalk);
            ensurePositiveRetries(dingTalk.getMaxRetries(), "DINGTALK");
            validateConfig(dingTalk, "DINGTALK");
            return dingTalk;
        }

        if (senderType == IMChannelType.WECHAT_WORK) {
            Map<String, Object> wechatWorkValues = resolveProtocolValues(safe, WECHAT_WORK_KEYS, "wechatWork", "wechat_work");
            WechatWorkIMChannelConfig wechatWork = mapToConfig(wechatWorkValues, WechatWorkIMChannelConfig.class, "WECHAT_WORK");
            ensurePositiveRetries(wechatWork.getMaxRetries(), "WECHAT_WORK");
            validateConfig(wechatWork, "WECHAT_WORK");
            return wechatWork;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的 IM 协议类型");
    }

    private Object resolveProtocolValue(Map<String, Object> safe) {
        Object protocolValue = safe.get("type");
        if (protocolValue == null || String.valueOf(protocolValue).trim().isBlank()) {
            protocolValue = safe.get("protocol");
        }
        return protocolValue;
    }

    private void applyDingTalkDerivedFields(DingTalkIMChannelConfig dingTalk) {
        String accessToken = extractAccessToken(dingTalk.getWebhookUrl());
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "钉钉 Webhook URL 缺少 access_token");
        }
        dingTalk.setAccessToken(accessToken);
    }

    private void ensurePositiveRetries(int maxRetries, String protocol) {
        if (maxRetries <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " maxRetries 必须大于 0");
        }
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

    private <T> T mapToConfig(Map<String, Object> values, Class<T> targetType, String protocol) {
        try {
            return objectMapper.convertValue(values, targetType);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " 配置格式非法", ex);
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
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, protocol + " 配置校验失败: " + message);
    }

    private String extractAccessToken(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return "";
        }
        String query = extractQuery(webhookUrl);
        if (query.isBlank()) {
            return "";
        }
        String[] queryParams = query.split("&");
        for (String param : queryParams) {
            String[] pair = param.split("=", 2);
            if (pair.length == 0) {
                continue;
            }
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            if (!"access_token".equals(key)) {
                continue;
            }
            if (pair.length == 1) {
                return "";
            }
            return URLDecoder.decode(pair[1], StandardCharsets.UTF_8).trim();
        }
        return "";
    }

    private String extractQuery(String webhookUrl) {
        try {
            URI uri = URI.create(webhookUrl);
            String query = uri.getRawQuery();
            return query == null ? "" : query;
        } catch (IllegalArgumentException ex) {
            int queryIndex = webhookUrl.indexOf('?');
            if (queryIndex < 0 || queryIndex >= webhookUrl.length() - 1) {
                return "";
            }
            return webhookUrl.substring(queryIndex + 1);
        }
    }

    private IMChannelType parseProtocol(Object value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IM 协议类型不能为空");
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IM 协议类型不能为空");
        }
        try {
            return IMChannelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IM 协议不支持: " + normalized);
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