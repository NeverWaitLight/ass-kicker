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
@CompoundIndex(name = "uk_t_localized_template_id_provider", def = "{'localizedTemplateId': 1, 'provider': 1}", unique = true)
@Document(collection = "provider_template")
public class ProviderTemplateEntity {
    @Id
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
