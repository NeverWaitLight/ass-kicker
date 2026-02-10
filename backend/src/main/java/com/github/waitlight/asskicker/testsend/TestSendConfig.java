package com.github.waitlight.asskicker.testsend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TestSendProperties.class)
public class TestSendConfig {
}