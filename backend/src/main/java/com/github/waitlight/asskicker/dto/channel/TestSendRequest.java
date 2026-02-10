package com.github.waitlight.asskicker.dto.channel;

import com.github.waitlight.asskicker.model.ChannelType;

import java.util.Map;

public record TestSendRequest(ChannelType type, Map<String, Object> properties, String target, String content) {
}