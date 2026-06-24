package com.github.waitlight.asskicker.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改当前用户信息")
public record UpdateMeDTO(
        @NotBlank(message = "用户名不能为空")
        @Schema(description = "用户名", example = "newusername")
        String username
) {
}
