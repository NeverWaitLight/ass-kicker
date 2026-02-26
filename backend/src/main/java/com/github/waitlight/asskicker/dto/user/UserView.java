package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;

public record UserView(
        String id,
        String username,
        UserRole role,
        UserStatus status,
        Long createdAt,
        Long updatedAt,
        Long lastLoginAt
) {
}
