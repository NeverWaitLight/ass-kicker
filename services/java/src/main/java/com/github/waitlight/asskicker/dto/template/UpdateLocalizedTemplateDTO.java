package com.github.waitlight.asskicker.dto.template;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LocalizedTemplate 更新 DTO，专门用于 TemplateController updateLocalized 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLocalizedTemplateDTO {

    @Size(max = 255)
    private String title;

    private String content;
}