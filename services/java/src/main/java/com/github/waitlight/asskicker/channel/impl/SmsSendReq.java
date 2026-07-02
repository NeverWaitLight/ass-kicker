package com.github.waitlight.asskicker.channel.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsSendReq extends SendReq {
    private String countryCode;
    private String phoneNumber;
    private String signName;
    private String templateCode;
    private Map<String, String> templateParam;
}
