package com.github.waitlight.asskicker.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "{user.username.notblank}")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{user.username.pattern}")
        String username,

        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 8, message = "{user.password.size}")
        String password
) {
}