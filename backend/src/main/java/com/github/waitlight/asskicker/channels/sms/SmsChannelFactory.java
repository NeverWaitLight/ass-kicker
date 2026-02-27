package com.github.waitlight.asskicker.channels.sms;

import com.github.waitlight.asskicker.channels.Channel;
import com.github.waitlight.asskicker.channels.ChannelConfig;
import com.github.waitlight.asskicker.channels.ChannelDebugSimulator;
import org.springframework.stereotype.Component;

@Component
public class SmsChannelFactory {

    private final ChannelDebugSimulator debugSimulator;

    public SmsChannelFactory(ChannelDebugSimulator debugSimulator) {
        this.debugSimulator = debugSimulator;
    }

    public Channel<?> create(ChannelConfig config) {
        if (config instanceof AliyunSmsChannelConfig aliyun) {
            return new AliyunSmsChannel(aliyun, debugSimulator);
        }
        if (config instanceof TencentSmsChannelConfig tencent) {
            return new TencentSmsChannel(tencent, debugSimulator);
        }
        throw new IllegalArgumentException("Unsupported SMS channel config: " + config);
    }
}
