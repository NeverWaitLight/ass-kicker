package com.github.waitlight.asskicker.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class StatusRouter {
  @Bean
  public RouterFunction<ServerResponse> statusRoutes() {
    return RouterFunctions.route(GET("/status"), request ->
        ServerResponse.ok().bodyValue(Map.of("service", "ass-kicker", "status", "OK"))
    );
  }
}
