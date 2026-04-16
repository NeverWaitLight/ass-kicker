package com.github.waitlight.asskicker.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelPropertyFieldVO {
    private String key;
    private String valueType;
    private boolean required;
}
