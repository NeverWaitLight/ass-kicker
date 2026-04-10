package com.github.waitlight.asskicker.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank(message = "{user.id.notblank}")
        String id,

        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 8, message = "{user.password.size}")
        String password,

        @NotBlank(message = "{user.currPassword.notblank}")
        @Size(min = 8, message = "{user.password.size}")
        String currPassword
) {
}