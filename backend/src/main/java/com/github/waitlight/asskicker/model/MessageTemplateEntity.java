package com.github.waitlight.asskicker.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "message_template")
public class MessageTemplateEntity {

        @Id
        private String id;

        @Indexed(name = "uk_message_template_code", unique = true)
        private String code;

        private ChannelType channelType;

        @Deprecated
        private JsonNode templates = JsonNodeFactory.instance.objectNode();
        @Deprecated
        private JsonNode channels = JsonNodeFactory.instance.objectNode();

        private Map<Language, LocalizedTemplate> localizedTemplates;

        private Long createdAt;
        private Long updatedAt;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LocalizedTemplate {
                private String title;
                private String content;
        }
}
