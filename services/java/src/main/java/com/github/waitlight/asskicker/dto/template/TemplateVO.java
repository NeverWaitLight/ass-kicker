package com.github.waitlight.asskicker.dto.template;

import com.github.waitlight.asskicker.model.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateVO {

    private String id;

    private String code;

    private String name;

    private ChannelType channelType;

    private boolean providerManaged;

    private Long createdAt;

    private Long updatedAt;
}
