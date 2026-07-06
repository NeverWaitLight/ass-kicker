package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DingTalkSendReq extends SendReq {
    private String token;
    private String secret;
    private String content;
}
