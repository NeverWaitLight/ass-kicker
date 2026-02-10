package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserRole;
import com.github.waitlight.asskicker.model.UserStatus;

public record CreateUserRequest(
        String username,
        String password,
        UserRole role,
        UserStatus status
) {
}
