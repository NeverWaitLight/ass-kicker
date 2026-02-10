package com.github.waitlight.asskicker.testsend;

import com.github.waitlight.asskicker.model.ChannelType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TemporaryChannelConfigManager {

    private final ConcurrentHashMap<String, TemporaryChannelConfig> configs = new ConcurrentHashMap<>();

    public TemporaryChannelConfig create(ChannelType type, Map<String, Object> properties) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> safeProperties = deepCopyMap(properties);
        TemporaryChannelConfig config = new TemporaryChannelConfig(id, type, safeProperties, Instant.now().toEpochMilli());
        configs.put(id, config);
        return config;
    }

    public TemporaryChannelConfig get(String id) {
        if (id == null) {
            return null;
        }
        return configs.get(id);
    }

    public void remove(String id) {
        if (id == null) {
            return;
        }
        configs.remove(id);
    }

    int size() {
        return configs.size();
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                result.put(entry.getKey(), deepCopyMap(castMap(mapValue)));
            } else if (value instanceof List<?> listValue) {
                result.put(entry.getKey(), deepCopyList(listValue));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private List<Object> deepCopyList(List<?> source) {
        List<Object> result = new ArrayList<>();
        for (Object value : source) {
            if (value instanceof Map<?, ?> mapValue) {
                result.add(deepCopyMap(castMap(mapValue)));
            } else if (value instanceof List<?> listValue) {
                result.add(deepCopyList(listValue));
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }
}