package com.github.waitlight.asskicker.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyDTO(
        @NotBlank
        @Size(max = 100)
        String name
) {
}
