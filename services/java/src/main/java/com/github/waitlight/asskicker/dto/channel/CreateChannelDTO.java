package com.github.waitlight.asskicker.dto.channel;

import java.util.HashMap;
import java.util.Map;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
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
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private ChannelType type;

    @NotNull
    private ChannelProvider provider;

    @Size(max = 1000)
    private String description;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}
