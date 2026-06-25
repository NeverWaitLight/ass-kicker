package com.github.waitlight.asskicker.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 全局字符串 trim 绑定配置，处理 {@code @RequestParam}、{@code @PathVariable}、{@code @ModelAttribute} 等参数。
 * <p>
 * trim 后保留空字符串 {@code ""}（不转为 {@code null}），确保 {@code @NotBlank} 等校验语义一致。
 */
@ControllerAdvice
public class GlobalStringTrimBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }
}
