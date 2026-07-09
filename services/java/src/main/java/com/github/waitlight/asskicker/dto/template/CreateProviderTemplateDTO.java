package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProviderTemplate 创建 DTO，专门用于 ProviderTemplateController createProvider 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProviderTemplateDTO {

    @NotBlank
    private String localizedTemplateId;

    @NotNull
    private ChannelProvider provider;

    private String providerTemplateCode;

    private Long uploadedAt;

    private String failureReason;
}
