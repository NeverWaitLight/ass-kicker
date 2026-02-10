package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.model.UserRole;

public record UserPrincipal(Long userId, UserRole role) {
}
