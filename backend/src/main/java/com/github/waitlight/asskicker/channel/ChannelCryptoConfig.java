package com.github.waitlight.asskicker.channel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChannelCryptoProperties.class)
public class ChannelCryptoConfig {
}