package com.github.waitlight.asskicker.sender.email;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class HttpEmailSenderConfig extends EmailSenderConfig {

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

    public HttpEmailSenderConfig() {
        super(EmailProtocol.HTTP);
    }
}
