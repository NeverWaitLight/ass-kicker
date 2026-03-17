package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record FillTemplateRequest(
        @NotBlank(message = "模板编码不能为空")
        String templateCode,
        @NotNull(message = "语言不能为空")
        Language language,
        Map<String, Object> params
) {
}
