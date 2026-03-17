package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.SendHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class SendRouter {

    @Bean
    public RouterFunction<ServerResponse> sendRoutes(SendHandler sendHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/api/send")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), sendHandler::send);
    }
}
