package com.github.waitlight.asskicker.router;

import com.github.waitlight.asskicker.handlers.SubmitHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class SubmitRouter {

    @Bean
    public RouterFunction<ServerResponse> submitRoutes(SubmitHandler submitHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/v1/submit")
                        .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)), submitHandler::submit);
    }
}
