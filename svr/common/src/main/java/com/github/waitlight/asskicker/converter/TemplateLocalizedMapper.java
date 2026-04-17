package com.github.waitlight.asskicker.converter;

import java.util.HashMap;
import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.waitlight.asskicker.model.Language;
import com.github.waitlight.asskicker.model.TemplateEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateLocalizedMapper {

    private final ObjectMapper objectMapper;

    @Named("localizedTemplatesToTemplatesJson")
    public JsonNode localizedTemplatesToTemplatesJson(Map<Language, TemplateEntity.LocalizedTemplate> map) {
        if (map == null || map.isEmpty()) {
            return JsonNodeFactory.instance.objectNode();
        }
        return objectMapper.valueToTree(map);
    }

    @Named("templatesJsonToLocalizedTemplates")
    public Map<Language, TemplateEntity.LocalizedTemplate> templatesJsonToLocalizedTemplates(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            return null;
        }
        if (node.size() == 0) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(node, new TypeReference<Map<Language, TemplateEntity.LocalizedTemplate>>() {});
    }
}
