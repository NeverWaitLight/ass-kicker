package com.github.waitlight.asskicker.dto;

import java.util.Map;

import com.github.waitlight.asskicker.model.Language;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UniMessageReq {
    private String templateCode;
    private Language language;
    private Map<String, Object> templateParams;
    private Map<String, Object> extraData;
}
