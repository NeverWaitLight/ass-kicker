package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.ChannelReq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsReq extends ChannelReq {
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

    /** 模板 ID,对应服务商已审核的模板编码 */
    @NotBlank
    private String templateId;

    /** 模板变量,占位符名到实际值的映射 */
    private Map<String, String> templateParam;
}
