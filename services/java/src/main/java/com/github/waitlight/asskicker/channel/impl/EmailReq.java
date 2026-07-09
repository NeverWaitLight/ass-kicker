package com.github.waitlight.asskicker.channel.impl;

import com.github.waitlight.asskicker.channel.SendReq;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class EmailReq extends SendReq {
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

    @Override
    public void applyRendered(String title, String content) {
        if (title != null && !title.isBlank()) this.subject = title;
        if (content != null && !content.isBlank()) this.body = content;
    }

    @Override
    public String recipient() {
        if (to == null || to.isEmpty()) return null;
        return String.join(",", to);
    }

    @Override
    public String renderedContent() {
        boolean hasSubject = subject != null && !subject.isBlank();
        boolean hasBody = body != null && !body.isBlank();
        if (hasSubject && hasBody) return subject + "\n" + body;
        if (hasSubject) return subject;
        if (hasBody) return body;
        return null;
    }
}
