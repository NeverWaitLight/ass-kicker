package com.github.waitlight.asskicker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Table("t_template")
public class Template {

    @Id
    private Long id;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    @Column("name")
    private String name;

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    @Column("code")
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column("description")
    private String description;

    @Column("created_at")
    private Long createdAt;

    @Column("updated_at")
    private Long updatedAt;

    @Transient
    private List<LanguageTemplate> languageTemplates = new ArrayList<>();

    public Template(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }
}
