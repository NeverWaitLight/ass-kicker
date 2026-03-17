package com.github.waitlight.asskicker.dto.submit;

import com.github.waitlight.asskicker.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SubmitRequest(
        @NotBlank(message = "模板编码不能为空")
        String templateCode,
        @NotNull(message = "语言不能为空")
        Language language,
        Map<String, Object> params,
        @NotEmpty(message = "收件人列表不能为空")
        List<String> recipients
) {
}
