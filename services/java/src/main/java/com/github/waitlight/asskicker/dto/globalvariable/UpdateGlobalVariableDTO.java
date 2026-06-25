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
public class UpdateGlobalVariableDTO {

    @NotBlank
    private String id;

    @Size(max = 100)
    private String key;

    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String value;

    @Size(max = 1000)
    private String description;

    private Boolean enabled;
}
