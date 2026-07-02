package com.github.waitlight.asskicker.dto.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ChannelDebugSendDTO {

    @NotBlank
    private String channelId;

    @NotNull
    private Map<String, Object> request;
}
