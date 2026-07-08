package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.waitlight.asskicker.channel.impl.EmailReq;
import com.github.waitlight.asskicker.channel.impl.ImReq;
import com.github.waitlight.asskicker.channel.impl.PushReq;
import com.github.waitlight.asskicker.channel.impl.SmsReq;
import com.github.waitlight.asskicker.model.ChannelProvider;
import com.github.waitlight.asskicker.model.ChannelType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "channelType", visible = true)
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

    private Map<String, String> templateParams;
}
