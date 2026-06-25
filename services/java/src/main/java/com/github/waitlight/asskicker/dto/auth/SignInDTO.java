package com.github.waitlight.asskicker.dto.auth;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录")
public record SignInDTO(
        @NotBlank
        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonDeserialize(using = StringDeserializer.class)
        String password
) {
}
