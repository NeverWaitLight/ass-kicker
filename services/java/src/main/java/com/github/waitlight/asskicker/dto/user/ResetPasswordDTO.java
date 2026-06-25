package com.github.waitlight.asskicker.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 8, message = "{user.password.size}")
        @JsonDeserialize(using = StringDeserializer.class)
        String newPassword,

        @NotBlank(message = "{user.currPassword.notblank}")
        @Size(min = 8, message = "{user.password.size}")
        @JsonDeserialize(using = StringDeserializer.class)
        String currPassword
) {
}