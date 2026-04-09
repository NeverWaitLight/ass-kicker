package com.github.waitlight.asskicker.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.annotation.PostConstruct;

@Configuration(proxyBeanMethods = false)
public class SpringDocLongAsStringConfig {

    @PostConstruct
    public void configure() {
        SpringDocUtils.getConfig()
                .replaceWithSchema(Long.class, new StringSchema().format("int64"))
                .replaceWithSchema(Long.TYPE, new StringSchema().format("int64"));
    }
}
