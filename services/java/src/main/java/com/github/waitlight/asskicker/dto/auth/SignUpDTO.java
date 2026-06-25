package com.github.waitlight.asskicker.dto.auth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "创建用户")
public record SignUpDTO(
        @NotBlank
        @Size(min = 1, max = 256)
        @Pattern(regexp = "^[a-zA-Z0-9]+$")
        String username,

        @NotBlank
        @Size(min = 8, max = 256)
        @JsonDeserialize(using = StringDeserializer.class)
        String password
) {
}