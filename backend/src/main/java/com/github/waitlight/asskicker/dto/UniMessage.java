package com.github.waitlight.asskicker.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UniMessage {
    private String title;
    private String content;
    private Map<String, Object> extraData;
}
