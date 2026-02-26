package com.github.waitlight.asskicker.dto.sender;

import com.github.waitlight.asskicker.model.SenderType;

import java.util.Map;

public record TestSendRequest(SenderType type, Map<String, Object> properties, String target, String content) {
}
