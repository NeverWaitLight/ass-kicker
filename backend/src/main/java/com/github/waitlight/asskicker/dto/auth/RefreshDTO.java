package com.github.waitlight.asskicker.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshDTO(
        @NotBlank(message = "{auth.refreshToken.notblank}")
        String refreshToken
) {
}