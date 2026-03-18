package com.github.waitlight.asskicker.dto.apikey;

import com.github.waitlight.asskicker.model.ApiKeyStatus;

public record CreateApiKeyResponse(
        String id,
        String name,
        String keyPrefix,
        String rawKey,
        Long expiresAt,
        ApiKeyStatus status,
        Long createdAt
) {
}
