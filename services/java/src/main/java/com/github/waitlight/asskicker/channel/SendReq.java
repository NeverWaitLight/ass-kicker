package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.Data;

@Data
public abstract class SendReq {
    protected ChannelType channelType;
    protected ChannelProvider provider;
}
