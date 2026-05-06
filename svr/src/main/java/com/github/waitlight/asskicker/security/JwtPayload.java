package com.github.waitlight.asskicker.security;

import com.github.waitlight.asskicker.model.UserRole;

public record JwtPayload(String userId, UserRole role, JwtTokenType tokenType) {
}
