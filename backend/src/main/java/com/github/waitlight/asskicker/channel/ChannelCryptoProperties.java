package com.github.waitlight.asskicker.channel;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.channel.crypto")
public class ChannelCryptoProperties {

    @NotBlank
    private String secret;

    private List<String> sensitiveKeys = new ArrayList<>(List.of(
            "password",
            "secret",
            "token",
            "apikey",
            "api_key",
            "accesskey",
            "privatekey"
    ));

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<String> getSensitiveKeys() {
        return sensitiveKeys;
    }

    public void setSensitiveKeys(List<String> sensitiveKeys) {
        if (sensitiveKeys == null) {
            this.sensitiveKeys = new ArrayList<>();
            return;
        }
        this.sensitiveKeys = new ArrayList<>(sensitiveKeys);
    }
}