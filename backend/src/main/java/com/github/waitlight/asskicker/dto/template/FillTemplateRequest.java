package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "模板填充请求")
public record FillTemplateRequest(
        @NotBlank(message = "模板编码不能为空")
        @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String templateCode,
        @NotNull(message = "语言不能为空")
        @Schema(description = "语言", requiredMode = Schema.RequiredMode.REQUIRED)
        Language language,
        @Schema(description = "模板变量，用于占位符替换")
        Map<String, Object> params
) {
}
