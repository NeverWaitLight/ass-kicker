package com.github.waitlight.asskicker.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.Map;

@Configuration
public class MongoJsonNodeConfig {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Bean
    MongoCustomConversions mongoCustomConversions(ObjectMapper objectMapper) {
        return new MongoCustomConversions(List.of(
                new JsonNodeToDocumentConverter(objectMapper),
                new DocumentToJsonNodeConverter(objectMapper)
        ));
    }

    @WritingConverter
    static class JsonNodeToDocumentConverter implements Converter<JsonNode, Document> {

        private final ObjectMapper objectMapper;

        JsonNodeToDocumentConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Document convert(JsonNode source) {
            if (source == null || source.isNull() || source.isMissingNode()) {
                return new Document();
            }
            Map<String, Object> values = objectMapper.convertValue(source, MAP_TYPE);
            return new Document(values);
        }
    }

    @ReadingConverter
    static class DocumentToJsonNodeConverter implements Converter<Document, JsonNode> {

        private final ObjectMapper objectMapper;

        DocumentToJsonNodeConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode convert(Document source) {
            if (source == null) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.valueToTree(source);
        }
    }
}
