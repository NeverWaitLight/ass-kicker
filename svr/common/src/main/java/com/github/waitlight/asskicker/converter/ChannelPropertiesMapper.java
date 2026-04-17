package com.github.waitlight.asskicker.converter;

import java.util.HashMap;
import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChannelPropertiesMapper {

    private final ObjectMapper objectMapper;

    @Named("channelPropertiesToJson")
    public JsonNode channelPropertiesToJson(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return JsonNodeFactory.instance.objectNode();
        }
        return objectMapper.valueToTree(properties);
    }

    @Named("channelObjectPropertiesToProperties")
    public Map<String, String> channelObjectPropertiesToProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @Named("channelJsonToProperties")
    public Map<String, String> channelJsonToProperties(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, String>>() {});
    }
}