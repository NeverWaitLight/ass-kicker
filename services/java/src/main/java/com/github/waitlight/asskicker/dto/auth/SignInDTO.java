package com.github.waitlight.asskicker.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "用户登录")
public record SignInDTO(
        @NotBlank(message = "{user.username.notblank}")
        @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank(message = "{user.password.notblank}")
        @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
}
