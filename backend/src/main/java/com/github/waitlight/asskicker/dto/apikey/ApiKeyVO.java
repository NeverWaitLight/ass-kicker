package com.github.waitlight.asskicker.dto.apikey;

import com.github.waitlight.asskicker.model.ApiKeyStatus;

public record ApiKeyVO(
        String id,
        String userId,
        String name,
        String maskedRawKey,
        Long expiresAt,
        ApiKeyStatus status,
        Long createdAt,
        Long revokedAt
) {
}