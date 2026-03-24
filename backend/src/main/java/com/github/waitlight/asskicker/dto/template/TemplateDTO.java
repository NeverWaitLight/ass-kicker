package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.LanguageTemplateEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDTO {

    private String id;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Template code is required")
    @Size(max = 100, message = "Template code must not exceed 100 characters")
    private String code;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private ChannelType channelType;

    private Map<String, String> attributes = new HashMap<>();

    private Long createdAt;

    private Long updatedAt;

    private List<LanguageTemplateEntity> languageTemplates = new ArrayList<>();
}
