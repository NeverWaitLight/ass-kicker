package com.github.waitlight.asskicker.dto.send;

import com.github.waitlight.asskicker.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record SendDTO(
        @NotBlank(message = "{send.templateCode.notblank}")
        String templateCode,
        @NotNull(message = "{send.language.notnull}")
        Language language,
        Map<String, Object> params,
        @NotEmpty(message = "{send.recipients.notempty}")
        List<String> recipients,
        String taskId,
        Long submittedAt
) {
}