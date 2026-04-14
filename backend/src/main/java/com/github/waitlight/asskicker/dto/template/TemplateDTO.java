package com.github.waitlight.asskicker.dto.template;

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
public class TemplateDTO {

    @NotBlank(message = "{template.id.notblank}")
    private String id;

    @NotBlank(message = "Message template code is required")
    @Size(max = 100, message = "Message template code must not exceed 100 characters")
    private String code;

    @NotNull(message = "Message template channel type is required")
    private ChannelType channelType;

    @NotNull(message = "Templates are required")
    private JsonNode templates = JsonNodeFactory.instance.objectNode();

    private JsonNode channels = JsonNodeFactory.instance.objectNode();

    private Long createdAt;

    private Long updatedAt;
}