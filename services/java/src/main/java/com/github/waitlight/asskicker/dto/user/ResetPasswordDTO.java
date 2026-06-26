package com.github.waitlight.asskicker.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDTO(
        @NotBlank
        @Size(min = 8)
        @JsonDeserialize(using = StringDeserializer.class)
        String newPassword
) {
}
