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

    @NotBlank(message = "{globalVariable.id.notblank}")
    private String id;

    @Size(max = 100, message = "{globalVariable.key.size}")
    private String key;

    @Size(max = 255, message = "{globalVariable.name.size}")
    private String name;

    @Size(max = 2000, message = "{globalVariable.value.size}")
    private String value;

    @Size(max = 1000, message = "{globalVariable.description.size}")
    private String description;

    private Boolean enabled;
}
