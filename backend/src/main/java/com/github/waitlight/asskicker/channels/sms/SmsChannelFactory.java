package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelFactory {

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof AliyunSmsChannelConfig aliyun) {
            return new AliyunSmsChannel(aliyun);
        }
        if (config instanceof TencentSmsChannelConfig tencent) {
            return new TencentSmsChannel(tencent);
        }
        throw new IllegalArgumentException("Unsupported SMS channel config: " + config);
    }
}
