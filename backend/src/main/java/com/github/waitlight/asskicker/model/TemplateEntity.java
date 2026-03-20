package com.github.waitlight.asskicker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_template")
public class TemplateEntity {

    @Id
    private String id;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    @Field("name")
    private String name;

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    @Field("code")
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Field("description")
    private String description;

    @Field("channel_type")
    private ChannelType channelType;

    @Field("content_type")
    private TemplateContentType contentType;

    @Field("created_at")
    private Long createdAt;

    @Field("updated_at")
    private Long updatedAt;

    @Transient
    private List<LanguageTemplateEntity> languageTemplates = new ArrayList<>();

    public TemplateEntity(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }
}
