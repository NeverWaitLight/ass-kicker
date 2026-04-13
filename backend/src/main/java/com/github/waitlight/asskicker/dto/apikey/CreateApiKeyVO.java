package com.github.waitlight.asskicker.dto.apikey;

import com.github.waitlight.asskicker.model.ApiKeyStatus;

public record CreateApiKeyVO(
        String id,
        String name,
        String rawKey,
        Long expiresAt,
        ApiKeyStatus status,
        Long createdAt
) {
}
