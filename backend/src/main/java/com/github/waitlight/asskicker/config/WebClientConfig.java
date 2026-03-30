package com.github.waitlight.asskicker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 提供项目统一注入的出站 {@link WebClient}。
 * <p>
 * Bean 方法名为 {@code webClient}，默认 Bean 名称为 {@code webClient}，不覆盖 Reactor Netty 与 codec 的默认配置。
 */
@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
