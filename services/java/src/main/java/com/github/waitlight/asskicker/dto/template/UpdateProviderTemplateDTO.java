package com.github.waitlight.asskicker.dto.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProviderTemplate 更新 DTO，专门用于 ProviderTemplateController updateProvider 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProviderTemplateDTO {

    private String providerTemplateCode;

    private Long uploadedAt;

    private String failureReason;
}
