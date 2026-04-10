package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserStatus;
import com.github.waitlight.asskicker.validation.Create;
import com.github.waitlight.asskicker.validation.ResetPassword;
import com.github.waitlight.asskicker.validation.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDTO(
        @NotBlank(message = "{user.id.notblank}", groups = {Update.class, ResetPassword.class})
        String id,

        @NotBlank(message = "{user.username.notblank}", groups = Create.class)
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{user.username.pattern}")
        String username,

        @NotBlank(message = "{user.password.notblank}", groups = {Create.class, ResetPassword.class})
        @Size(min = 8, message = "{user.password.size}")
        String password,

        @NotBlank(message = "{user.currPassword.notblank}", groups = ResetPassword.class)
        @Size(min = 8, message = "{user.password.size}")
        String currPassword,

        UserStatus status
) {
}