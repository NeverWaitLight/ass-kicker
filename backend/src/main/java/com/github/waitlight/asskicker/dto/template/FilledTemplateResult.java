package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.TemplateEntity;

public record FilledTemplateResult(TemplateEntity template, String renderedContent) {
}
