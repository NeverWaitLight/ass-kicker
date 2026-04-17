package com.github.waitlight.asskicker.dto.channel;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelPropertiesSchemaVO {
    private List<ChannelPropertyFieldVO> properties;
}
