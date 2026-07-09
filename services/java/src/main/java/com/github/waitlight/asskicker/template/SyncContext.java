package com.github.waitlight.asskicker.template;

import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.LocalizedTemplateEntity;
import com.github.waitlight.asskicker.model.TemplateEntity;
import lombok.Builder;

@Builder
public record SyncContext(
        TemplateEntity template,
        LocalizedTemplateEntity localized,
        ChannelEntity channel,
        String existingProviderTemplateCode,
        Integer smsTemplateType,
        Boolean international,
        String remark,
        String signName) {
}
