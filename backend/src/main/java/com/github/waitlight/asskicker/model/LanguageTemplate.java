package com.github.waitlight.asskicker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_language_template")
public class LanguageTemplate {

    @Id
    private String id;

    @NotNull
    @Field("template_id")
    private String templateId;

    @NotNull
    @Field("language")
    private Language language;

    @NotBlank
    @Field("content")
    private String content;

    @Field("created_at")
    private Long createdAt;

    @Field("updated_at")
    private Long updatedAt;

    public LanguageTemplate(String templateId, Language language, String content) {
        this.templateId = templateId;
        this.language = language;
        this.content = content;
    }
}
