package com.github.waitlight.asskicker.dto.auth;

public record RegisterRequest(
        String username,
        String password
) {
}
