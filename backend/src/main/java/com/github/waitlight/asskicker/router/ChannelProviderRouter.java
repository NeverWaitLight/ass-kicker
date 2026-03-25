package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handler.ChannelProviderHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class ChannelProviderRouter {

        @Bean
        public RouterFunction<ServerResponse> channelProviderRoutes(ChannelProviderHandler channelProviderHandler) {
                return RouterFunctions
                                .route(RequestPredicates.POST("/v1/channel-providers")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::create)
                                .andRoute(RequestPredicates.GET("/v1/channel-providers")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::listPage)
                                .andRoute(RequestPredicates.GET("/v1/channel-providers/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::getById)
                                .andRoute(RequestPredicates.PUT("/v1/channel-providers/{id}")
                                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                                                channelProviderHandler::update)
                                .andRoute(RequestPredicates.DELETE("/v1/channel-providers/{id}"),
                                                channelProviderHandler::delete);
        }
}
