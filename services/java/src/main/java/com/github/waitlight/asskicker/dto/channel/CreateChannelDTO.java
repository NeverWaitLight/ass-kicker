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

    @NotBlank
    @Size(max = 100)
    private String key;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ChannelType type;

    @NotNull
    private ProviderType provider;

    @Size(max = 1000)
    private String description;

    @Size(max = 2048)
    private String priorityAddressRegex;

    @Size(max = 2048)
    private String excludeAddressRegex;

    @Builder.Default
    private boolean enabled = true;

    @Valid
    private ChannelRateLimitConfig rateLimit;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}
