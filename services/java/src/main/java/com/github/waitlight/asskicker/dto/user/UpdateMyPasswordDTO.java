package com.github.waitlight.asskicker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改当前用户密码")
public record UpdateMyPasswordDTO(
        @NotBlank(message = "当前密码不能为空")
        @Schema(description = "当前密码", example = "currentpassword123")
        String currPassword,

        @NotBlank(message = "新密码不能为空")
        @Schema(description = "新密码", example = "newpassword123")
        String newPassword
) {
}
