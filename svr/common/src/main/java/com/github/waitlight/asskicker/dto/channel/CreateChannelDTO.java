package com.github.waitlight.asskicker.dto.channel;

import java.util.HashMap;
import java.util.Map;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import com.github.waitlight.asskicker.model.ChannelRateLimitConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Channel 创建 DTO，专门用于 ChannelController create 方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChannelDTO {

    @NotBlank(message = "{channel.key.notblank}")
    @Size(max = 100, message = "{channel.key.size}")
    private String key;

    @NotBlank(message = "{channel.name.notblank}")
    @Size(max = 255, message = "{channel.name.size}")
    private String name;

    @NotNull(message = "{channel.type.notnull}")
    private ChannelType type;

    @NotNull(message = "{channel.provider.notnull}")
    private ProviderType provider;

    @Size(max = 1000, message = "{channel.description.size}")
    private String description;

    @Size(max = 2048, message = "{channel.priorityAddressRegex.size}")
    private String priorityAddressRegex;

    @Size(max = 2048, message = "{channel.excludeAddressRegex.size}")
    private String excludeAddressRegex;

    @Builder.Default
    private boolean enabled = true;

    @Valid
    private ChannelRateLimitConfig rateLimit;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}
