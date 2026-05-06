package com.github.waitlight.asskicker.dto.apikey;

public record CreateApiKeyVO(
        String id,
        String name,
        String rawKey,
        Long createdAt
) {
}
