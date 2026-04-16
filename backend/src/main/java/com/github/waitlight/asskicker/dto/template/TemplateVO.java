package com.github.waitlight.asskicker.dto.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVO {

    private String id;

    private String code;

    private ChannelType channelType;

    @Builder.Default
    private JsonNode templates = JsonNodeFactory.instance.objectNode();

    @Builder.Default
    private JsonNode channels = JsonNodeFactory.instance.objectNode();

    private Long createdAt;

    private Long updatedAt;
}
