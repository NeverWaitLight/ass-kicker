package com.github.waitlight.asskicker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@CompoundIndex(name = "uk_t_template_id_language", def = "{'templateId': 1, 'language': 1}", unique = true)
@Document(collection = "localized_templates")
public class LocalizedTemplateEntity {
    @Id
    private String id;
    private String templateId;
    private Language language;
    private String title;
    private String content;
    private String creator;
    private String updater;
    private Long createdAt;
    private Long updatedAt;
}