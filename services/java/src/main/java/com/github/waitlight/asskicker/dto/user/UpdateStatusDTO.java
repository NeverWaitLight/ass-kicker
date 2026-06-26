package com.github.waitlight.asskicker.dto.user;

import com.github.waitlight.asskicker.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusDTO(
        @NotNull
        UserStatus status
) {
}
