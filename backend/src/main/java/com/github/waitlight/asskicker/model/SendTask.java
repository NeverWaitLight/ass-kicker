package com.github.waitlight.asskicker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendTask {

    private String taskId;
    private String templateCode;
    private String languageCode;
    private Map<String, Object> params;
    private String channelId;
    private List<String> recipients;
    private Long submittedAt;
}
