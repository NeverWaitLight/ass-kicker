package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handler.ChannelConfigHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ChannelConfigRouter {

    @Bean
    public RouterFunction<ServerResponse> channelRoutes(ChannelConfigHandler channelConfigHandler) {
        return RouterFunctions
                        .route(RequestPredicates.POST("/v1/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::createChannel)
                        .andRoute(RequestPredicates.GET("/v1/channels")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::listChannels)
                        .andRoute(RequestPredicates.GET("/v1/channels/types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::listChannelTypes)
                        .andRoute(RequestPredicates.GET("/v1/channels/im-types")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::listImTypes)
                        .andRoute(RequestPredicates.GET("/v1/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::getChannelById)
                        .andRoute(RequestPredicates.PUT("/v1/channels/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::updateChannel)
                        .andRoute(RequestPredicates.DELETE("/v1/channels/{id}"), channelConfigHandler::deleteChannel)
                        .andRoute(RequestPredicates.POST("/v1/channels/test-send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), channelConfigHandler::testSend);
    }
}
