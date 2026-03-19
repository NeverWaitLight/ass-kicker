package com.github.waitlight.asskicker.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.waitlight.asskicker.channel.email.EmailChannelConfigConverter;
import com.github.waitlight.asskicker.channel.email.HttpEmailChannel;
import com.github.waitlight.asskicker.channel.email.HttpEmailChannelProperties;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannel;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannelProperties;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannel;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannelProperties;
import com.github.waitlight.asskicker.channel.im.IMChannelConfigConverter;
import com.github.waitlight.asskicker.channel.im.WeComIMChannel;
import com.github.waitlight.asskicker.channel.im.WeComIMChannelProperties;
import com.github.waitlight.asskicker.channel.push.APNsPushChannel;
import com.github.waitlight.asskicker.channel.push.APNsPushChannelProperties;
import com.github.waitlight.asskicker.channel.push.FCMPushChannel;
import com.github.waitlight.asskicker.channel.push.FCMPushChannelProperties;
import com.github.waitlight.asskicker.channel.push.PushChannelConfigConverter;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannel;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannelProperties;
import com.github.waitlight.asskicker.channel.sms.SmsChannelConfigConverter;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannel;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import com.github.waitlight.asskicker.model.ChannelConfig;
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
    private final EmailChannelConfigConverter emailChannelConfigConverter;
    private final IMChannelConfigConverter imChannelConfigConverter;
    private final PushChannelConfigConverter pushChannelConfigConverter;
    private final SmsChannelConfigConverter smsChannelConfigConverter;

    public Channel<?> create(ChannelConfig channelConfig) {
        Map<String, Object> properties = getPropertiesFromConfig(channelConfig);
        return create(channelConfig.getType(), properties);
    }

    public Channel<?> create(ChannelType type, Map<String, Object> properties) {
        ChannelProperties config = toProperties(type, properties);
        return create(config);
    }

    public Channel<?> create(ChannelProperties config) {
        if (config instanceof HttpEmailChannelProperties http) {
            return new HttpEmailChannel(http, sharedWebClient, debugProperties);
        }
        if (config instanceof SmtpEmailChannelProperties smtp) {
            return new SmtpEmailChannel(smtp, debugProperties);
        }
        if (config instanceof DingTalkIMChannelProperties dingTalk) {
            return new DingTalkIMChannel(dingTalk, sharedWebClient, debugProperties);
        }
        if (config instanceof WeComIMChannelProperties wechatWork) {
            return new WeComIMChannel(wechatWork, sharedWebClient, debugProperties);
        }
        if (config instanceof APNsPushChannelProperties apns) {
            return new APNsPushChannel(apns, debugProperties);
        }
        if (config instanceof FCMPushChannelProperties fcm) {
            return new FCMPushChannel(fcm, sharedWebClient, debugProperties);
        }
        if (config instanceof AliyunSmsChannelProperties aliyun) {
            return new AliyunSmsChannel(aliyun, debugProperties);
        }
        if (config instanceof TencentSmsChannelProperties tencent) {
            return new TencentSmsChannel(tencent, debugProperties);
        }
        throw new IllegalArgumentException("Unsupported channel config: " + config);
    }

    private ChannelProperties toProperties(ChannelType type, Map<String, Object> properties) {
        Map<String, Object> safe = properties != null ? properties : new LinkedHashMap<>();
        if (type == ChannelType.EMAIL) {
            return emailChannelConfigConverter.fromProperties(safe);
        }
        if (type == ChannelType.IM) {
            return imChannelConfigConverter.fromProperties(safe);
        }
        if (type == ChannelType.PUSH) {
            return pushChannelConfigConverter.fromProperties(safe);
        }
        if (type == ChannelType.SMS) {
            return smsChannelConfigConverter.fromProperties(safe);
        }
        throw new IllegalArgumentException("Unsupported channel type: " + type);
    }

    private Map<String, Object> getPropertiesFromConfig(ChannelConfig channelConfig) {
        Map<String, Object> properties = channelConfig.getProperties();
        if (properties != null && !properties.isEmpty()) {
            return properties;
        }
        return readProperties(channelConfig.getPropertiesJson());
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
