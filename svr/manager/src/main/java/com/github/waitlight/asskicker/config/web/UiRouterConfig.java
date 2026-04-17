package com.github.waitlight.asskicker.config.web;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class UiRouterConfig {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/v1/",
            "/actuator/",
            "/scalar",
            "/scalar/",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/swagger-ui",
            "/swagger-ui/");

    @Bean
    RouterFunction<ServerResponse> spaIndexRoute() {
        ClassPathResource index = new ClassPathResource("static/index.html");
        return RouterFunctions.route(RequestPredicates.GET("/**")
                        .and(RequestPredicates.accept(MediaType.TEXT_HTML))
                        .and(this::isSpaRoute), request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(BodyInserters.fromResource(index)));
    }

    private boolean isSpaRoute(ServerRequest request) {
        String path = request.path();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return true;
        }
        if (EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith)) {
            return false;
        }
        return !path.substring(1).contains(".");
    }
}
