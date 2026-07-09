package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalizedTemplateVO {

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