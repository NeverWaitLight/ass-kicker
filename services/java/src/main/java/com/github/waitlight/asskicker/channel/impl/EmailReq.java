package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailReq extends SendReq {
    /** 发件人地址,留空则回退到 channel 配置里的 from */
    private String from;
    /** 收件人列表,必填且不能为空 */
    private List<String> to;
    /** 抄送地址列表,可空 */
    private List<String> cc;
    /** 密送地址列表,可空 */
    private List<String> bcc;
    /** 邮件主题 */
    private String subject;
    /** 邮件正文(纯文本) */
    private String body;
}
