package com.github.waitlight.asskicker.sender.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.sender.mail")
@Data
public class EmailSenderProperties {

    /**
     * 当前启用的邮件协议
     */
    @NotNull
    private EmailProtocolType protocol = EmailProtocolType.SMTP;

    /**
     * SMTP 协议配置
     */
    @Valid
    private Smtp smtp = new Smtp();

    /**
     * HTTP API 协议配置
     */
    @Valid
    private HttpApi httpApi = new HttpApi();

    @Data
    public static class Smtp {

        @NotBlank
        private String host;

        @Min(1)
        private int port = 465;

        @NotBlank
        private String username;

        @NotBlank
        private String password;

        /**
         * 协议名称一般为 smtp
         */
        @NotBlank
        private String protocol = "smtp";

        /**
         * 是否启用 SSL
         */
        private boolean sslEnabled = true;

        /**
         * 发件人地址为空时默认使用 username
         */
        private String from;

        /**
         * 连接超时时间
         */
        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(5);

        /**
         * 读取超时时间
         */
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(10);

        /**
         * 最大重试次数
         */
        @Min(0)
        private int maxRetries = 3;

        /**
         * 重试间隔时间
         */
        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);
    }

    @Data
    public static class HttpApi {

        /**
         * 邮件服务 HTTP API 基础地址
         * 例如 https://example.com
         */
        @NotBlank
        private String baseUrl;

        /**
         * 发送邮件的路径
         * 例如 /api/mail/send
         */
        @NotBlank
        private String path;

        /**
         * 鉴权头部名称例如 Authorization 或 X Api Key
         */
        @NotBlank
        private String apiKeyHeader = "Authorization";

        /**
         * 鉴权凭证内容
         */
        @NotBlank
        private String apiKey;

        /**
         * 发件人地址为空时可以在服务端侧使用默认值
         */
        private String from;

        /**
         * HTTP 调用超时时间
         */
        @NotNull
        private Duration timeout = Duration.ofSeconds(5);

        /**
         * 最大重试次数
         */
        @Min(0)
        private int maxRetries = 3;

        /**
         * 重试间隔时间
         */
        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);
    }
}

