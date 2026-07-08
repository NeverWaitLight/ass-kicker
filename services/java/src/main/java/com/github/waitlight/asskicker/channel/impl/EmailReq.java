package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.ChannelReq;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailReq extends ChannelReq {
    /** 发件人地址,留空则回退到 channel 配置里的 from */
    @Email
    private String from;

    /** 收件人列表,必填且不能为空 */
    @NotEmpty
    private List<@NotBlank @Email String> to;

    /** 抄送地址列表,可空 */
    private List<@NotBlank @Email String> cc;

    /** 密送地址列表,可空 */
    private List<@NotBlank @Email String> bcc;

    /** 邮件主题 */
    @NotBlank
    private String subject;

    /** 邮件正文(纯文本) */
    @NotBlank
    private String body;
}
