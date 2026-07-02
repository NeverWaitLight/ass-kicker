package com.github.waitlight.asskicker.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDebugResultVO {

    private boolean success;
    private String result;
    private String error;

    public static ChannelDebugResultVO success(String result) {
        return new ChannelDebugResultVO(true, result, null);
    }

    public static ChannelDebugResultVO error(String error) {
        return new ChannelDebugResultVO(false, null, error);
    }
}
