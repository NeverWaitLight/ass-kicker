package com.github.waitlight.asskicker.dto.record;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.SendRecordStatus;

import java.util.Map;

public record RecordVO(
        String id,
        String taskId,
        String templateCode,
        String languageCode,
        Map<String, Object> params,
        String channelId,
        String recipient,
        Long submittedAt,
        String renderedContent,
        ChannelType channelType,
        String channelName,
        SendRecordStatus status,
        String errorCode,
        String errorMessage,
        Long sentAt
) {
}
