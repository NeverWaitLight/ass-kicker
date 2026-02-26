package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.model.UserRole;

public record UserPrincipal(String userId, UserRole role) {
}
