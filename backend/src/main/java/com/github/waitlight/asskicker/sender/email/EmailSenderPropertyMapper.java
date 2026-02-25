package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.SenderConfig;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class EmailSenderPropertyMapper {

    private static final Set<String> SMTP_KEYS = Set.of(
            "host", "port", "username", "password",
            "sslEnabled", "from", "connectionTimeout", "readTimeout",
            "maxRetries", "retryDelay"
    );
    private static final Set<String> HTTP_API_KEYS = Set.of(
            "baseUrl", "path", "apiKeyHeader", "apiKey",
            "from", "timeout", "maxRetries", "retryDelay"
    );

    public SenderConfig fromProperties(Map<String, Object> properties) {
        Map<String, Object> safe = normalizeProperties(properties);

        EmailProtocol protocol = parseProtocol(safe.get("protocol"));
        if (protocol == EmailProtocol.HTTP) {
            HttpEmailSenderConfig httpApi = new HttpEmailSenderConfig();
            Map<String, Object> httpApiValues = mergeProtocolValues(readMap(safe.get("httpApi")), safe, HTTP_API_KEYS);
            applyHttpApi(httpApi, httpApiValues);
            return httpApi;
        }
        SmtpEmailSenderConfig smtp = new SmtpEmailSenderConfig();
        Map<String, Object> smtpValues = mergeProtocolValues(readMap(safe.get("smtp")), safe, SMTP_KEYS);
        applySmtp(smtp, smtpValues);
        return smtp;
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

    private void applySmtp(SmtpEmailSenderConfig smtp, Map<String, Object> values) {
        String host = readString(values, "host");
        String username = readString(values, "username");
        String password = readString(values, "password");

        if (host.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP 主机不能为空");
        }
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP 用户名不能为空");
        }
        if (password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP 密码不能为空");
        }

        smtp.setHost(host);
        smtp.setUsername(username);
        smtp.setPassword(password);
        smtp.setPort(readInt(values, "port", smtp.getPort(), "SMTP 端口非法"));
        smtp.setSslEnabled(readBoolean(values, "sslEnabled", smtp.isSslEnabled()));
        smtp.setFrom(readString(values, "from", smtp.getFrom()));
        smtp.setConnectionTimeout(readDuration(values, "connectionTimeout", smtp.getConnectionTimeout()));
        smtp.setReadTimeout(readDuration(values, "readTimeout", smtp.getReadTimeout()));
        smtp.setMaxRetries(readInt(values, "maxRetries", smtp.getMaxRetries(), "SMTP 最大重试次数非法"));
        smtp.setRetryDelay(readDuration(values, "retryDelay", smtp.getRetryDelay()));
    }

    private void applyHttpApi(HttpEmailSenderConfig httpApi, Map<String, Object> values) {
        String baseUrl = readString(values, "baseUrl");
        String path = readString(values, "path");
        String apiKeyHeader = readString(values, "apiKeyHeader");
        String apiKey = readString(values, "apiKey");

        if (baseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP API 基础地址不能为空");
        }
        if (path.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP API 发送路径不能为空");
        }
        if (apiKeyHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP API 鉴权头不能为空");
        }
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP API 鉴权令牌不能为空");
        }

        httpApi.setBaseUrl(baseUrl);
        httpApi.setPath(path);
        httpApi.setApiKeyHeader(apiKeyHeader);
        httpApi.setApiKey(apiKey);
        httpApi.setFrom(readString(values, "from", httpApi.getFrom()));
        httpApi.setTimeout(readDuration(values, "timeout", httpApi.getTimeout()));
        httpApi.setMaxRetries(readInt(values, "maxRetries", httpApi.getMaxRetries(), "HTTP API 最大重试次数非法"));
        httpApi.setRetryDelay(readDuration(values, "retryDelay", httpApi.getRetryDelay()));
    }

    private EmailProtocol parseProtocol(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return EmailProtocol.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮件协议不支持 " + normalized);
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

    private boolean readBoolean(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).trim().isBlank()) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "1".equals(text) || "yes".equals(text)) {
            return true;
        }
        if ("false".equals(text) || "0".equals(text) || "no".equals(text)) {
            return false;
        }
        return fallback;
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
