package com.github.waitlight.asskicker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_JWT = "bearer-jwt";
    public static final String API_KEY = "api-key";

    @Bean
    public OpenAPI assKickerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("ass-kicker API").description("ass-kicker WebFlux API").version("0.0.1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Authorization Bearer access token"))
                        .addSecuritySchemes(API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("可与 JWT 二选一用于 /v1/send 与 /v1/submit")));
    }
}
