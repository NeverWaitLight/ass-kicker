package com.github.waitlight.asskicker.sendercrypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SenderCryptoProperties.class)
public class SenderCryptoConfig {
}
