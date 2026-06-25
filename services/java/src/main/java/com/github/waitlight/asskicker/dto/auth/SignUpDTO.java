package com.github.waitlight.asskicker.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "创建用户")
public record SignUpDTO(
        @NotBlank(message = "{user.username.notblank}")
        @Size(min = 1, max = 256, message = "{user.username.size}")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{user.username.pattern}")
        @Schema(description = "用户名，仅允许字母和数字，长度 1 到 256 位", minLength = 1, maxLength = 256, requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 8, max = 256, message = "{user.password.size}")
        @Schema(description = "密码，长度 8 到 256 位",  minLength = 8, maxLength = 256, requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}