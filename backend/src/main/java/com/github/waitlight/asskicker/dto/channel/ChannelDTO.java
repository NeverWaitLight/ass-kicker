package com.github.waitlight.asskicker.dto.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDTO {

    @NotBlank(message = "{channel.id.notblank}")
    private String id;

    @NotBlank(message = "Channel provider key is required")
    @Size(max = 100, message = "Channel provider key must not exceed 100 characters")
    private String key;

    @NotBlank(message = "Channel provider name is required")
    @Size(max = 255, message = "Channel provider name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Channel type is required")
    private ChannelType type;

    @NotBlank(message = "Provider is required")
    @Size(max = 100, message = "Provider must not exceed 100 characters")
    private String provider;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 2048, message = "Priority address regex must not exceed 2048 characters")
    private String priorityAddressRegex;

    @Size(max = 2048, message = "Exclude address regex must not exceed 2048 characters")
    private String excludeAddressRegex;

    private boolean enabled = true;

    @NotNull(message = "Properties are required")
    private JsonNode properties = JsonNodeFactory.instance.objectNode();

    private Long createdAt;

    private Long updatedAt;
}
