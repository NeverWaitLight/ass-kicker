package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderTemplateVO {

    private String id;

    private String localizedTemplateId;

    private ChannelProvider provider;

    private String providerTemplateCode;

    private Long uploadedAt;

    private String failureReason;

    private String creator;

    private String updater;

    private Long createdAt;

    private Long updatedAt;
}
