package com.github.waitlight.asskicker.router;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration(proxyBeanMethods = false)
public class HealthRouter {
  @Bean
  public RouterFunction<ServerResponse> healthRoutes() {
    return RouterFunctions.route(GET("/health"), request ->
        ServerResponse.ok().bodyValue(Map.of("status", "UP"))
    );
  }
}
