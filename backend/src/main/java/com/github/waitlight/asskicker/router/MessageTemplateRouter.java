package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handler.MessageTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class MessageTemplateRouter {

    @Bean
    public RouterFunction<ServerResponse> messageTemplateRoutes(MessageTemplateHandler messageTemplateHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/v1/message-templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        messageTemplateHandler::create)
                .andRoute(RequestPredicates.GET("/v1/message-templates")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        messageTemplateHandler::listPage)
                .andRoute(RequestPredicates.GET("/v1/message-templates/code/{code}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        messageTemplateHandler::getByCode)
                .andRoute(RequestPredicates.GET("/v1/message-templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        messageTemplateHandler::getById)
                .andRoute(RequestPredicates.PUT("/v1/message-templates/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        messageTemplateHandler::update)
                .andRoute(RequestPredicates.DELETE("/v1/message-templates/{id}"),
                        messageTemplateHandler::delete);
    }
}
