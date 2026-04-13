package com.github.waitlight.asskicker.dto.apikey;

public record ApiKeyVO(
        String id,
        String userId,
        String name,
        String maskedRawKey,
        Long createdAt
) {
}
