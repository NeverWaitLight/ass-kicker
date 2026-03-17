package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.ChannelHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ChannelRouter {

    @Bean
    public RouterFunction<ServerResponse> channelRoutes(ChannelHandler channelHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::createChannel)
                .andRoute(RequestPredicates.GET("/api/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::listChannels)
                .andRoute(RequestPredicates.GET("/api/channels/types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::listChannelTypes)
                .andRoute(RequestPredicates.GET("/api/channels/im-types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::listImTypes)
                .andRoute(RequestPredicates.GET("/api/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::getChannelById)
                .andRoute(RequestPredicates.PUT("/api/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::updateChannel)
                .andRoute(RequestPredicates.DELETE("/api/channels/{id}"), channelHandler::deleteChannel)
                .andRoute(RequestPredicates.POST("/api/channels/test-send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelHandler::testSend);
    }
}
