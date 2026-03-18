package com.github.waitlight.asskicker.dto.apikey;

import com.github.waitlight.asskicker.model.ApiKeyStatus;

public record ApiKeyView(
        String id,
        String name,
        String keyPrefix,
        Long expiresAt,
        ApiKeyStatus status,
        Long createdAt,
        Long revokedAt
) {
}
