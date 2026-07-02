package com.github.waitlight.asskicker.dto.channel;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Channel 更新 DTO，专门用于 ChannelController update 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChannelDTO {

    @NotBlank
    private String id;

    @Size(max = 100)
    private String code;

    @Size(max = 255)
    private String name;

    private ChannelType type;

    private ChannelProvider provider;

    @Size(max = 1000)
    private String description;

    private Boolean enabled;

    private Map<String, Object> properties;
}
