package com.github.waitlight.asskicker.dto.channel;

import java.util.HashMap;
import java.util.Map;

import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestChannelDTO {

    @NotNull
    private ChannelType type;

    /**
     * 可选 若同一通道类型下有多家服务商则必须指定
     */
    private ProviderType provider;

    @NotBlank
    private String target;

    @NotBlank
    private String content;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}
