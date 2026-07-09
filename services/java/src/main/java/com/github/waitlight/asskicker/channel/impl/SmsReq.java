package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsReq extends SendReq {
    /** 国家/地区代码,不含 + 号,如 86;国内运营商可省略 */
    @Pattern(regexp = "^\\d{1,4}$")
    private String countryCode;

    /** 手机号码,不含国家代码 */
    @NotBlank
    @Pattern(regexp = "^\\d{5,15}$")
    private String phoneNumber;

    /** 短信签名,对应服务商控制台已审核通过的签名名称 */
    @NotBlank
    private String signName;

    @Override
    public void applyRendered(String title, String content) {
        // SMS 正文由服务商模板管理，本地无内容字段可填充
    }

    @Override
    public String recipient() {
        if (phoneNumber == null) return null;
        return (countryCode != null && !countryCode.isBlank())
                ? "+" + countryCode + phoneNumber
                : phoneNumber;
    }

    @Override
    public String renderedContent() {
        return null;
    }
}
