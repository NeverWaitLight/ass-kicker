package com.github.waitlight.asskicker.channels.email;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpEmailChannelConfig extends EmailChannelConfig {

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

    public HttpEmailChannelConfig() {
        super(EmailChannelType.HTTP);
    }
}
