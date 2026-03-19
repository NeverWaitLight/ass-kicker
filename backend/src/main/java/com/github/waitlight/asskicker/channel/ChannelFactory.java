package com.github.waitlight.asskicker.channel;

import com.github.waitlight.asskicker.channel.email.HttpEmailChannel;
import com.github.waitlight.asskicker.channel.email.HttpEmailChannelProperties;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannel;
import com.github.waitlight.asskicker.channel.email.SmtpEmailChannelProperties;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannel;
import com.github.waitlight.asskicker.channel.im.DingTalkIMChannelProperties;
import com.github.waitlight.asskicker.channel.im.WeComIMChannel;
import com.github.waitlight.asskicker.channel.im.WeComIMChannelProperties;
import com.github.waitlight.asskicker.channel.push.APNsPushChannel;
import com.github.waitlight.asskicker.channel.push.APNsPushChannelProperties;
import com.github.waitlight.asskicker.channel.push.FCMPushChannel;
import com.github.waitlight.asskicker.channel.push.FCMPushChannelProperties;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannel;
import com.github.waitlight.asskicker.channel.sms.AliyunSmsChannelProperties;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannel;
import com.github.waitlight.asskicker.channel.sms.TencentSmsChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ChannelFactory {

    private final WebClient sharedWebClient;
    private final ChannelDebugProperties debugProperties;

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
}
