package com.github.waitlight.asskicker.testsend;

import com.github.waitlight.asskicker.model.ChannelType;

import java.util.Map;

public record TemporaryChannelConfig(String id, ChannelType type, Map<String, Object> properties, long createdAt) {
}
