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

    private String id;

    @NotBlank(message = "{channel.key.notblank}")
    @Size(max = 100, message = "{channel.key.size}")
    private String key;

    @NotBlank(message = "{channel.name.notblank}")
    @Size(max = 255, message = "{channel.name.size}")
    private String name;

    @NotNull(message = "{channel.type.notnull}")
    private ChannelType type;

    @NotBlank(message = "{channel.provider.notblank}")
    @Size(max = 100, message = "{channel.provider.size}")
    private String provider;

    @Size(max = 1000, message = "{channel.description.size}")
    private String description;

    @Size(max = 2048, message = "{channel.priorityAddressRegex.size}")
    private String priorityAddressRegex;

    @Size(max = 2048, message = "{channel.excludeAddressRegex.size}")
    private String excludeAddressRegex;

    private boolean enabled = true;

    @NotNull
    private JsonNode properties = JsonNodeFactory.instance.objectNode();

    private Long createdAt;

    private Long updatedAt;
}
