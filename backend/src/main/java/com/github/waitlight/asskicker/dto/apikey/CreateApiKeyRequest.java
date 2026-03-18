package com.github.waitlight.asskicker.dto.apikey;

public record CreateApiKeyRequest(
        String name,
        Long expiresAt
) {
}
