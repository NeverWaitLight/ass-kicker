package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_template")
public class TemplateEntity {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("code")
    private String code;

    @Field("description")
    private String description;

    @Field("channel_type")
    private ChannelType channelType;

    @Field("attributes")
    private Map<String, String> attributes = new HashMap<>();

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
