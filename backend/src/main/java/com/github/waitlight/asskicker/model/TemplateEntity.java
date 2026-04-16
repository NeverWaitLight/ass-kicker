package com.github.waitlight.asskicker.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "template")
public class TemplateEntity {

        @Id
        private String id;

        @Indexed(name = "uk_template_code", unique = true)
        private String code;

        private String name;

        private ChannelType channelType;

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