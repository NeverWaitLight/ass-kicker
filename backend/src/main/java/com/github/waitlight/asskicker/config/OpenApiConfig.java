package com.github.waitlight.asskicker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("!native")
@OpenAPIDefinition(
        info = @Info(
                title = "Ass Kicker API",
                version = "v1",
                description = "Ass Kicker 后端接口文档"
        )
)
public class OpenApiConfig {
}
