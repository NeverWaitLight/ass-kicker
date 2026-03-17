package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler userHandler) {
        return RouterFunctions
                        .route(RequestPredicates.POST("/v1/users")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::createUser)
                        .andRoute(RequestPredicates.GET("/v1/users")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::listUsers)
                        .andRoute(RequestPredicates.GET("/v1/users/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::getUserById)
                        .andRoute(RequestPredicates.DELETE("/v1/users/{id}"), userHandler::deleteUser)
                        .andRoute(RequestPredicates.PUT("/v1/users/{id}/password")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::resetPassword)
                        .andRoute(RequestPredicates.PATCH("/v1/users/me")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::updateMeUsername)
                        .andRoute(RequestPredicates.PUT("/v1/users/me/password")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), userHandler::updateMePassword);
    }
}
