package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "t_channel_config")
public class ChannelEntity {

    @Id
    private String id;

    @NotBlank(message = "Channel name is required")
    @Size(max = 255, message = "Channel name must not exceed 255 characters")
    @Field("name")
    private String name;

    @NotNull(message = "Channel type is required")
    @Field("type")
    private ChannelType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Field("description")
    private String description;

    @Size(max = 2048, message = "Include recipient regex must not exceed 2048 characters")
    @Field("include_recipient_regex")
    private String includeRecipientRegex;

    @Size(max = 2048, message = "Exclude recipient regex must not exceed 2048 characters")
    @Field("exclude_recipient_regex")
    private String excludeRecipientRegex;

    @JsonIgnore
    @Field("properties_json")
    private String propertiesJson;

    @Field("created_at")
    private Long createdAt;

    @Field("updated_at")
    private Long updatedAt;

    @Transient
    private Map<String, Object> properties = new LinkedHashMap<>();
}
