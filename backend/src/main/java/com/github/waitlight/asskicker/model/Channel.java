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
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Table("t_channel")
public class Channel {

    @Id
    private Long id;

    @NotBlank(message = "Channel name is required")
    @Size(max = 255, message = "Channel name must not exceed 255 characters")
    @Column("name")
    private String name;

    @NotNull(message = "Channel type is required")
    @Column("type")
    @Schema(description = "渠道类型", allowableValues = {"SMS", "EMAIL", "IM", "PUSH"})
    private ChannelType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Column("description")
    private String description;

    @JsonIgnore
    @Column("properties")
    private String propertiesJson;

    @Column("created_at")
    private Long createdAt;

    @Column("updated_at")
    private Long updatedAt;

    @Transient
    private Map<String, Object> properties = new LinkedHashMap<>();
}
