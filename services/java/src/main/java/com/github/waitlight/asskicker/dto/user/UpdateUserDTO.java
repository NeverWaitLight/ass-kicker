package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserDTO(
        @NotBlank(message = "{user.id.notblank}")
        String id,

        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{user.username.pattern}")
        String username,

        UserStatus status
) {
}