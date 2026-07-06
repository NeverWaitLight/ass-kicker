package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsReq extends SendReq {
    /** 国家/地区代码,不含 + 号,如 86;国内运营商可省略 */
    private String countryCode;
    /** 手机号码,不含国家代码 */
    private String phoneNumber;
    /** 短信签名,对应服务商控制台已审核通过的签名名称 */
    private String signName;
    /** 模板 ID,对应服务商已审核的模板编码 */
    private String templateId;
    /** 模板变量,占位符名到实际值的映射 */
    private Map<String, String> templateParam;
}
