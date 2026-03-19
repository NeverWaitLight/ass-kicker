package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.email.EmailChannelSpecConverter;
import com.github.waitlight.asskicker.channel.email.HttpEmailChannel;
import com.github.waitlight.asskicker.channel.email.HttpEmailChannelSpec;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannel;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannelSpec;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannel;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannelSpec;
import com.github.waitlight.asskicker.channel.im.IMChannelSpecConverter;
import com.github.waitlight.asskicker.channel.im.WeComIMChannel;
import com.github.waitlight.asskicker.channel.im.WeComIMChannelSpec;
import com.github.waitlight.asskicker.channel.push.APNsPushChannel;
import com.github.waitlight.asskicker.channel.push.APNsPushChannelSpec;
import com.github.waitlight.asskicker.channel.push.FCMPushChannel;
import com.github.waitlight.asskicker.channel.push.FCMPushChannelSpec;
import com.github.waitlight.asskicker.channel.push.PushChannelSpecConverter;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannel;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannelSpec;
import com.github.waitlight.asskicker.channel.sms.SmsChannelSpecConverter;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannel;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannelSpec;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import com.github.waitlight.asskicker.model.ChannelEntity;
import com.github.waitlight.asskicker.model.ChannelType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChannelFactory {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WebClient sharedWebClient;
    private final ChannelDebugProperties debugProperties;
    private final ObjectMapper objectMapper;
    private final EmailChannelSpecConverter emailChannelSpecConverter;
    private final IMChannelSpecConverter imChannelSpecConverter;
    private final PushChannelSpecConverter pushChannelSpecConverter;
    private final SmsChannelSpecConverter smsChannelSpecConverter;

    public Channel<?> create(ChannelEntity channelEntity) {
        Map<String, Object> properties = getPropertiesFromEntity(channelEntity);
        return create(channelEntity.getType(), properties);
    }

    public Channel<?> create(ChannelType type, Map<String, Object> properties) {
        ChannelSpec spec = toSpec(type, properties);
        return create(spec);
    }

    public Channel<?> create(ChannelSpec spec) {
        if (spec instanceof HttpEmailChannelSpec http) {
            return new HttpEmailChannel(http, sharedWebClient, debugProperties);
        }
        if (spec instanceof SmtpEmailChannelSpec smtp) {
            return new SmtpEmailChannel(smtp, debugProperties);
        }
        if (spec instanceof DingTalkIMChannelSpec dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient, debugProperties);
        }
        if (spec instanceof WeComIMChannelSpec wechatWork) {
            return new WeComIMChannel(wechatWork, sharedWebClient, debugProperties);
        }
        if (spec instanceof APNsPushChannelSpec apns) {
            return new APNsPushChannel(apns, debugProperties);
        }
        if (spec instanceof FCMPushChannelSpec fcm) {
            return new FCMPushChannel(fcm, sharedWebClient, debugProperties);
        }
        if (spec instanceof AliyunSmsChannelSpec aliyun) {
            return new AliyunSmsChannel(aliyun, debugProperties);
        }
        if (spec instanceof TencentSmsChannelSpec tencent) {
            return new TencentSmsChannel(tencent, debugProperties);
        }
        throw new IllegalArgumentException("Unsupported channel spec: " + spec);
    }

    private ChannelSpec toSpec(ChannelType type, Map<String, Object> properties) {
        Map<String, Object> safe = properties != null ? properties : new LinkedHashMap<>();
        if (type == ChannelType.EMAIL) {
            return emailChannelSpecConverter.fromProperties(safe);
        }
        if (type == ChannelType.IM) {
            return imChannelSpecConverter.fromProperties(safe);
        }
        if (type == ChannelType.PUSH) {
            return pushChannelSpecConverter.fromProperties(safe);
        }
        if (type == ChannelType.SMS) {
            return smsChannelSpecConverter.fromProperties(safe);
        }
        throw new IllegalArgumentException("Unsupported channel type: " + type);
    }

    private Map<String, Object> getPropertiesFromEntity(ChannelEntity channelEntity) {
        Map<String, Object> properties = channelEntity.getProperties();
        if (properties != null && !properties.isEmpty()) {
            return properties;
        }
        return readProperties(channelEntity.getPropertiesJson());
    }

    private Map<String, Object> readProperties(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse channel properties: " + ex.getMessage(), ex);
        }
    }
}
