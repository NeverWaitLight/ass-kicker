package com.github.waitlight.asskicker.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改当前用户密码")
public record UpdateMyPasswordDTO(
        @NotBlank(message = "当前密码不能为空")
        @Schema(description = "当前密码", example = "currentpassword123")
        @JsonDeserialize(using = StringDeserializer.class)
        String currPassword,

        @NotBlank(message = "新密码不能为空")
        @Schema(description = "新密码", example = "newpassword123")
        @JsonDeserialize(using = StringDeserializer.class)
        String newPassword
) {
}
