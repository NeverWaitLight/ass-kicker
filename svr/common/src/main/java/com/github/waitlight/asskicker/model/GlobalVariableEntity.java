package com.github.waitlight.asskicker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "global_variables")
public class GlobalVariableEntity {

    @Id
    private String id;

    @Indexed(name = "uk_global_variable_key", unique = true)
    private String key;

    private String name;

    private String value;

    private String description;

    private Boolean enabled;

    private Long createdAt;

    private Long updatedAt;
}
