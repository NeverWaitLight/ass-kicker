package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserStatus;
import jakarta.validation.constraints.Pattern;

public record UserDTO(
        @Pattern(regexp = "^[a-zA-Z0-9]+$")
        String username,

        UserStatus status
) {
}