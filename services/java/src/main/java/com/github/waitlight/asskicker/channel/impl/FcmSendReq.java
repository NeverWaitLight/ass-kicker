package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class FcmSendReq extends SendReq {
    private List<String> deviceTokens;
    private String title;
    private String body;
    private Map<String, Object> data;
    private String priority;
}
