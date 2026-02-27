package com.github.waitlight.asskicker.channels;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MsgReq {

    private String recipient;
    private String subject;
    private String content;
    private Map<String, Object> attributes;
}