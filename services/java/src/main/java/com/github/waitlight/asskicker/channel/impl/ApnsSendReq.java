package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApnsSendReq extends SendReq {
    private List<String> deviceTokens;
    private String title;
    private String body;
    private Integer badge;
    private String sound;
    private Map<String, Object> customData;
}
