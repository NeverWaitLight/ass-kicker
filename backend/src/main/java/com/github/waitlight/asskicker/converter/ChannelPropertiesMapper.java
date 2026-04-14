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

    @Named("channelProviderPropertiesToJson")
    public JsonNode channelProviderPropertiesToJson(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return JsonNodeFactory.instance.objectNode();
        }
        return objectMapper.valueToTree(properties);
    }

    @Named("channelProviderJsonToProperties")
    public Map<String, String> channelProviderJsonToProperties(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, String>>() {});
    }
}
