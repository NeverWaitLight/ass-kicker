package com.github.waitlight.asskicker.dto;

import java.util.Map;

import com.github.waitlight.asskicker.model.Language;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UniMessage {
    private String templateCode;
    private Language language;
    private Map<String, Object> templateParams;
    private String title;
    private String content;
    private Map<String, Object> extraData;
}
