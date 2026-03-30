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
    /**
     * 原始模板参数，供阿里云短信等需要 JSON 占位符的通道使用。
     */
    private Map<String, Object> templateParams;
}
