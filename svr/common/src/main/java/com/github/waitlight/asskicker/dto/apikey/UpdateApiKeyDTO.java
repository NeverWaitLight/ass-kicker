package com.github.waitlight.asskicker.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateApiKeyDTO(
        @NotBlank(message = "{apikey.id.notblank}")
        String id,
        @NotBlank(message = "{apikey.name.notblank}")
        @Size(max = 100, message = "{apikey.name.size}")
        String name
) {
}
