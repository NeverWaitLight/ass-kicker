package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.model.ChannelType;
import lombok.Data;

@Data
public class SendReq {
    protected ChannelType channelType;
}
