package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.AuthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class AuthRouter {

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler authHandler) {
        return RouterFunctions
                        .route(RequestPredicates.POST("/v1/auth/login")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), authHandler::login)
                        .andRoute(RequestPredicates.POST("/v1/auth/register")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), authHandler::register)
                        .andRoute(RequestPredicates.POST("/v1/auth/refresh")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), authHandler::refresh)
                        .andRoute(RequestPredicates.GET("/v1/auth/me")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), authHandler::me);
    }
}
