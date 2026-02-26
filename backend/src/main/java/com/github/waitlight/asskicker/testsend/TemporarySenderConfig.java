package com.github.waitlight.asskicker.testsend;

import com.github.waitlight.asskicker.model.SenderType;

import java.util.Map;

public record TemporarySenderConfig(String id, SenderType type, Map<String, Object> properties, long createdAt) {
}
