package com.github.waitlight.asskicker.dto.submit;

import com.github.waitlight.asskicker.model.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@Schema(description = "提交发送任务请求")
public record SubmitRequest(
        @NotBlank(message = "模板编码不能为空")
        @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String templateCode,
        @NotNull(message = "语言不能为空")
        @Schema(description = "语言", requiredMode = Schema.RequiredMode.REQUIRED)
        Language language,
        @Schema(description = "模板参数，用于占位符替换")
        Map<String, Object> params,
        @NotEmpty(message = "收件人列表不能为空")
        @Schema(description = "收件人列表", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> recipients
) {
}
