package com.github.waitlight.asskicker.sender.email;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmtpEmailSenderConfig extends EmailSenderConfig {

    @NotBlank
    private String host;

    @Min(1)
    private int port = 465;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

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

    public SmtpEmailSenderConfig() {
        super(EmailSenderType.SMTP);
    }
}