package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.SendRecordHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class SendRecordRouter {

    @Bean
    public RouterFunction<ServerResponse> sendRecordRoutes(SendRecordHandler sendRecordHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/api/send-records")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), sendRecordHandler::listRecords)
                .andRoute(RequestPredicates.GET("/api/send-records/{id}")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), sendRecordHandler::getRecordById);
    }
}
