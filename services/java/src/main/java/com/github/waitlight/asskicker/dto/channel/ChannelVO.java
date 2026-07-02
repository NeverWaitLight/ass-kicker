package com.github.waitlight.asskicker.dto.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
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

    private String code;

    private String name;

    private ChannelType type;

    private ChannelProvider provider;

    private String description;

    private boolean enabled;

    @Builder.Default
    private JsonNode properties = JsonNodeFactory.instance.objectNode();

    private String creator;

    private String updater;

    private Long createdAt;

    private Long updatedAt;
}
