package com.github.waitlight.asskicker.dto.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Channel VO，专门用于 Controller 响应给前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelVO {

    private String id;

    private String key;

    private String name;

    private ChannelType type;

    private ProviderType provider;

    private String description;

    private String priorityAddressRegex;

    private String excludeAddressRegex;

    private boolean enabled;

    @Builder.Default
    private JsonNode properties = JsonNodeFactory.instance.objectNode();

    private Long createdAt;

    private Long updatedAt;
}