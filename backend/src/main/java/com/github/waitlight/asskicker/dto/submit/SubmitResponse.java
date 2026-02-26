package com.github.waitlight.asskicker.dto.submit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "提交发送任务响应")
public record SubmitResponse(
        @Schema(description = "任务ID")
        String taskId
) {
}
