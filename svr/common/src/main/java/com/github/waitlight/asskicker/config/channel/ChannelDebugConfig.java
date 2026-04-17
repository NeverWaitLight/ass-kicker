package com.github.waitlight.asskicker.config.channel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChannelDebugProperties.class)
public class ChannelDebugConfig {
}
