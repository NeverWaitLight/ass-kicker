package com.github.waitlight.asskicker.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "{user.username.notblank}")
        String username,

        @NotBlank(message = "{user.password.notblank}")
        String password
) {
}