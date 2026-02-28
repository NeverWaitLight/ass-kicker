package com.github.waitlight.asskicker.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "模板填充响应")
public record FillTemplateResponse(
        @Schema(description = "填充后的模板内容")
        String content
) {
}
