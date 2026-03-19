package com.github.waitlight.asskicker.channel.sms;

import com.github.waitlight.asskicker.channel.Channel;
import com.github.waitlight.asskicker.channel.ChannelProperties;
import com.github.waitlight.asskicker.config.ChannelDebugProperties;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelFactory {

    private final ChannelDebugProperties debugProperties;

    public SmsChannelFactory(ChannelDebugProperties debugProperties) {
        this.debugProperties = debugProperties;
    }

    public Channel<?> create(ChannelProperties config) {
        if (config instanceof AliyunSmsChannelProperties aliyun) {
            return new AliyunSmsChannel(aliyun, debugProperties);
        }
        if (config instanceof TencentSmsChannelProperties tencent) {
            return new TencentSmsChannel(tencent, debugProperties);
        }
        throw new IllegalArgumentException("Unsupported SMS channel config: " + config);
    }
}
