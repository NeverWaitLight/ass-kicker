package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handler.ChannelEntityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ChannelEntityRouter {

    @Bean
    public RouterFunction<ServerResponse> channelRoutes(ChannelEntityHandler channelEntityHandler) {
        return RouterFunctions
                        .route(RequestPredicates.POST("/v1/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::createChannel)
                        .andRoute(RequestPredicates.GET("/v1/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::listChannels)
                        .andRoute(RequestPredicates.GET("/v1/channels/types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::listChannelTypes)
                        .andRoute(RequestPredicates.GET("/v1/channels/im-types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::listImTypes)
                        .andRoute(RequestPredicates.GET("/v1/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::getChannelById)
                        .andRoute(RequestPredicates.PUT("/v1/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::updateChannel)
                        .andRoute(RequestPredicates.DELETE("/v1/channels/{id}"), channelEntityHandler::deleteChannel)
                        .andRoute(RequestPredicates.POST("/v1/channels/test-send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelEntityHandler::testSend);
    }
}
