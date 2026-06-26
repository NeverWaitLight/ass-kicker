package com.github.waitlight.asskicker.model;

/**
 * 服务商所属的厂商维度
 * 以便按阿里云、AWS、钉钉等厂商进行聚合、统计与权限管理
 */
public enum Provider {
    /** 阿里云 */
    ALIYUN,
    /** 亚马逊云服务（Amazon Web Services） */
    AWS,
    /** 钉钉 */
    DINGTALK,
    /** 企业微信（企微） */
    WECOM,
    /** 飞书 */
    FEISHU,
    /** Apple */
    APPLE,
    /** Google */
    GOOGLE,
    ;
}
