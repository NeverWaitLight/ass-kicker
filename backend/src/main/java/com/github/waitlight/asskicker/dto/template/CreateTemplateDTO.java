package com.github.waitlight.asskicker.dto.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template 创建 DTO，专门用于 TemplateController create 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateDTO {

    @NotBlank(message = "Message template code is required")
    @Size(max = 100, message = "Message template code must not exceed 100 characters")
    private String code;

    @NotNull(message = "Message template channel type is required")
    private ChannelType channelType;

    @NotNull(message = "Templates are required")
    @Builder.Default
    private JsonNode templates = JsonNodeFactory.instance.objectNode();

    @Builder.Default
    private JsonNode channels = JsonNodeFactory.instance.objectNode();
}
