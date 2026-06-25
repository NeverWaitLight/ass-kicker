package com.github.waitlight.asskicker.dto.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template 更新 DTO，专门用于 TemplateController update 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTemplateDTO {

    @NotBlank
    private String id;

    @Size(max = 100)
    private String code;

    @Size(max = 255)
    private String name;

    private ChannelType channelType;

    private JsonNode templates;

    private JsonNode channels;
}
