package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.ApiKeyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ApiKeyRouter {

    @Bean
    public RouterFunction<ServerResponse> apiKeyRoutes(ApiKeyHandler apiKeyHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/v1/api-keys")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), apiKeyHandler::createApiKey)
                .andRoute(RequestPredicates.GET("/v1/api-keys")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), apiKeyHandler::listApiKeys)
                .andRoute(RequestPredicates.DELETE("/v1/api-keys/{id}"), apiKeyHandler::revokeApiKey);
    }
}
