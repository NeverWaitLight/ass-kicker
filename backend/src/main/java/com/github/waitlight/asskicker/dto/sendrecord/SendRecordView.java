package com.github.waitlight.asskicker.dto.sendrecord;

import com.github.waitlight.asskicker.model.ChannelType;

import java.util.List;
import java.util.Map;

public record SendRecordView(
        String id,
        String taskId,
        String templateCode,
        String languageCode,
        Map<String, Object> params,
        String channelId,
        List<String> recipients,
        String recipient,
        Long submittedAt,
        String renderedContent,
        ChannelType channelType,
        String channelName,
        Boolean success,
        String errorCode,
        String errorMessage,
        Long sentAt
) {
}
