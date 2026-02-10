package com.github.waitlight.asskicker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@Table("t_language_template")
public class LanguageTemplate {

    @Id
    private Long id;

    @NotNull
    @Column("template_id")
    private Long templateId;

    @NotNull
    @Column("language")
    private Language language;

    @NotBlank
    @Column("content")
    private String content;

    @Column("created_at")
    private Long createdAt;

    @Column("updated_at")
    private Long updatedAt;

    public LanguageTemplate(Long templateId, Language language, String content) {
        this.templateId = templateId;
        this.language = language;
        this.content = content;
    }
}
