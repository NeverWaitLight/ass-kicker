package com.github.waitlight.asskicker.sender.email;

import com.github.waitlight.asskicker.sender.SenderProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.Duration;

@Data
public class EmailSenderProperty implements SenderProperty {

    @NotNull
    private EmailProtocolType protocol = EmailProtocolType.SMTP;

    @Valid
    private Smtp smtp = new Smtp();

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

        @NotBlank
        private String protocol = "smtp";

        private boolean sslEnabled = true;

        private String from;

        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(5);

        @NotNull
        private Duration readTimeout = Duration.ofSeconds(10);

        @Min(0)
        private int maxRetries = 3;

        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);
    }

    @Data
    public static class HttpApi {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String path;

        @NotBlank
        private String apiKeyHeader = "Authorization";

        @NotBlank
        private String apiKey;

        private String from;

        @NotNull
        private Duration timeout = Duration.ofSeconds(5);

        @Min(0)
        private int maxRetries = 3;

        @NotNull
        private Duration retryDelay = Duration.ofSeconds(1);
    }
}
