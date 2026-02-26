package com.github.waitlight.asskicker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Document(collection = "t_channel")
public class Channel {

    @Id
    private String id;

    @NotBlank(message = "Channel name is required")
    @Size(max = 255, message = "Channel name must not exceed 255 characters")
    @Field("name")
    private String name;

    @NotNull(message = "Channel type is required")
    @Field("type")
    @Schema(description = "通道类型", allowableValues = {"SMS", "EMAIL", "IM", "PUSH"})
    private ChannelType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Field("description")
    private String description;

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
