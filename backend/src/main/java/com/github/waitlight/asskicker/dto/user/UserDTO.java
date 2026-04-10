package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.validation.Create;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDTO(
        String id,

        @NotBlank(message = "{user.username.notblank}", groups = Create.class)
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{user.username.pattern}")
        String username,

        @NotBlank(message = "{user.password.notblank}", groups = Create.class)
        @Size(min = 8, message = "{user.password.size}")
        String password,

        @Size(min = 8, message = "{user.currPassword.size}")
        String currPassword
) {
}