package com.github.waitlight.asskicker.dto.globalvariable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalVariableVO {

    private String id;

    private String key;

    private String name;

    private String value;

    private String description;

    private Boolean enabled;

    private Long createdAt;

    private Long updatedAt;
}
