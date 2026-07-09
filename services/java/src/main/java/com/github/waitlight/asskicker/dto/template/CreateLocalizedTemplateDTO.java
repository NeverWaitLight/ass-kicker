package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LocalizedTemplate 创建 DTO，专门用于 TemplateController createLocalized 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLocalizedTemplateDTO {

    @NotBlank
    private String templateId;

    @NotNull
    private Language language;

    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;
}