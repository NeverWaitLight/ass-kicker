package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsReq extends SendReq {
    private String countryCode;
    private String phoneNumber;
    private String signName;
    private String templateId;
    private Map<String, String> templateParam;
}
