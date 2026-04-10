package com.github.waitlight.asskicker.dto.apikey;

public record CreateApiKeyDTO(
        String name,
        Long expiresAt
) {
}