package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.dto.channel.EmailProtocolSchemaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailProtocolSchemaService {

    public EmailProtocolSchemaResponse getSchema() {
        EmailSenderProperties defaults = new EmailSenderProperties();
        EmailSenderProperties.Smtp smtpDefaults = defaults.getSmtp();
        EmailSenderProperties.HttpApi httpDefaults = defaults.getHttpApi();

        List<EmailProtocolSchemaResponse.EmailProtocolField> smtpFields = List.of(
                field("host", "SMTP 主机", "string", true, "", "smtp.example.com"),
                field("port", "端口", "number", true, String.valueOf(smtpDefaults.getPort()), "465"),
                field("username", "用户名", "string", true, "", "user@example.com"),
                field("password", "密码", "secret", true, "", ""),
                field("protocol", "传输协议", "string", false, smtpDefaults.getProtocol(), "smtp"),
                field("sslEnabled", "启用 SSL", "boolean", false, String.valueOf(smtpDefaults.isSslEnabled()), "true"),
                field("from", "发件人", "string", false, nullSafe(smtpDefaults.getFrom()), "user@example.com"),
                field("connectionTimeout", "连接超时(ms)", "duration", false,
                        String.valueOf(smtpDefaults.getConnectionTimeout().toMillis()), "5000"),
                field("readTimeout", "读取超时(ms)", "duration", false,
                        String.valueOf(smtpDefaults.getReadTimeout().toMillis()), "10000"),
                field("maxRetries", "最大重试次数", "number", false,
                        String.valueOf(smtpDefaults.getMaxRetries()), "3"),
                field("retryDelay", "重试间隔(ms)", "duration", false,
                        String.valueOf(smtpDefaults.getRetryDelay().toMillis()), "1000")
        );

        List<EmailProtocolSchemaResponse.EmailProtocolField> httpFields = List.of(
                field("baseUrl", "API 基础地址", "string", true, "", "https://api.example.com"),
                field("path", "发送路径", "string", true, "", "/api/mail/send"),
                field("apiKeyHeader", "鉴权头", "string", true, httpDefaults.getApiKeyHeader(), "Authorization"),
                field("apiKey", "鉴权令牌", "secret", true, "", ""),
                field("from", "发件人", "string", false, nullSafe(httpDefaults.getFrom()), "user@example.com"),
                field("timeout", "超时(ms)", "duration", false,
                        String.valueOf(httpDefaults.getTimeout().toMillis()), "5000"),
                field("maxRetries", "最大重试次数", "number", false,
                        String.valueOf(httpDefaults.getMaxRetries()), "3"),
                field("retryDelay", "重试间隔(ms)", "duration", false,
                        String.valueOf(httpDefaults.getRetryDelay().toMillis()), "1000")
        );

        EmailProtocolSchemaResponse.EmailProtocolSchema smtpSchema =
                new EmailProtocolSchemaResponse.EmailProtocolSchema(
                        EmailProtocolType.SMTP.name(),
                        "SMTP",
                        "smtp",
                        smtpFields
                );

        EmailProtocolSchemaResponse.EmailProtocolSchema httpSchema =
                new EmailProtocolSchemaResponse.EmailProtocolSchema(
                        EmailProtocolType.HTTP_API.name(),
                        "HTTP API",
                        "httpApi",
                        httpFields
                );

        return new EmailProtocolSchemaResponse(EmailProtocolType.SMTP.name(), List.of(smtpSchema, httpSchema));
    }

    private EmailProtocolSchemaResponse.EmailProtocolField field(String key,
                                                                 String label,
                                                                 String type,
                                                                 boolean required,
                                                                 String defaultValue,
                                                                 String placeholder) {
        return new EmailProtocolSchemaResponse.EmailProtocolField(
                key,
                label,
                type,
                required,
                defaultValue,
                placeholder
        );
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
