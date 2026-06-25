package com.github.waitlight.asskicker.dto.globalvariable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGlobalVariableDTO {

    @NotBlank
    @Size(max = 100, message = "{globalVariable.key.size}")
    private String key;

    @NotBlank
    @Size(max = 255, message = "{globalVariable.name.size}")
    private String name;

    @NotBlank
    @Size(max = 2000, message = "{globalVariable.value.size}")
    private String value;

    @Size(max = 1000, message = "{globalVariable.description.size}")
    private String description;

    @Builder.Default
    private Boolean enabled = true;
}
