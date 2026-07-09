package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template 创建 DTO，专门用于 TemplateController create 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateDTO {

    @NotBlank
    @Size(max = 100)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ChannelType channelType;

    /**
     * 是否将模板托管至服务商，由服务商负责渲染与发送
     */
    @Builder.Default
    private boolean providerManaged = false;
}
