package com.github.waitlight.asskicker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改当前用户密码")
public record UpdateMyPasswordDTO(
        @NotBlank(message = "旧密码不能为空")
        @Schema(description = "旧密码", example = "oldpassword123")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Schema(description = "新密码", example = "newpassword123")
        String newPassword
) {
}
