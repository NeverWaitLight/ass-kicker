package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class PushReq extends SendReq {
    private String deviceToken;
    private String title;
    private String body;
    private String priority;
    private Map<String, Object> data;
}
