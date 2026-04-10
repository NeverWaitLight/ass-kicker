package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.validation.Create;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDTO(
        String id,

        @NotBlank(message = "用户名不能为空", groups = Create.class)
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "用户名只能包含英文字母和数字")
        String username,

        @NotBlank(message = "密码不能为空", groups = Create.class)
        @Size(min = 8, message = "密码长度至少8位")
        String password,

        @Size(min = 8, message = "当前密码长度至少8位")
        String currPassword
) {
}