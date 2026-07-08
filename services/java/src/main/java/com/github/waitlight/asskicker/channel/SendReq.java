package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.waitlight.asskicker.channel.impl.EmailReq;
import com.github.waitlight.asskicker.channel.impl.ImReq;
import com.github.waitlight.asskicker.channel.impl.PushReq;
import com.github.waitlight.asskicker.channel.impl.SmsReq;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import com.github.waitlight.asskicker.model.Language;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SmsReq.class, name = "SMS"),
        @JsonSubTypes.Type(value = EmailReq.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = ImReq.class, name = "DINGTALK"),
        @JsonSubTypes.Type(value = ImReq.class, name = "FEISHU"),
        @JsonSubTypes.Type(value = PushReq.class, name = "APNS"),
        @JsonSubTypes.Type(value = PushReq.class, name = "FCM"),
})
public abstract class SendReq {
    @NotNull
    private ChannelType type;

    private ChannelProvider provider;

    private String templateCode;

    private Language language;

    private Map<String, String> templateParams;

    /**
     * 为 true 时直接发送子类已填充的正文字段，跳过模板引擎组装；
     * 为 false 时若 templateCode 非空则走模板渲染流程。
     */
    private boolean directSend = false;

    /**
     * 将模板引擎渲染后的 title / content 写入子类各自的内容字段；
     * 空值不覆盖，以兼容服务商托管模板（content 由服务商侧渲染）。
     */
    public abstract void applyRendered(String title, String content);
}
