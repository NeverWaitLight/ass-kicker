package com.github.waitlight.asskicker.dto.channel;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.model.ChannelRateLimitConfig;
import jakarta.validation.Valid;
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

    @NotBlank(message = "{channel.id.notblank}")
    private String id;

    @Size(max = 100, message = "{channel.key.size}")
    private String key;

    @Size(max = 255, message = "{channel.name.size}")
    private String name;

    private ChannelType type;

    private ProviderType provider;

    @Size(max = 1000, message = "{channel.description.size}")
    private String description;

    @Size(max = 2048, message = "{channel.priorityAddressRegex.size}")
    private String priorityAddressRegex;

    @Size(max = 2048, message = "{channel.excludeAddressRegex.size}")
    private String excludeAddressRegex;

    private Boolean enabled;

    @Valid
    private ChannelRateLimitConfig rateLimit;

    private Map<String, Object> properties;
}
