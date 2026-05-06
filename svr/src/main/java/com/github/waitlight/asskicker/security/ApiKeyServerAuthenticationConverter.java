package com.github.waitlight.asskicker.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ApiKeyServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String X_API_KEY = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String xApiKey = exchange.getRequest().getHeaders().getFirst(X_API_KEY);
        if (xApiKey != null && !xApiKey.isBlank()) {
            return Mono.just(new UsernamePasswordAuthenticationToken(null, xApiKey.trim()));
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return Mono.empty();
        }
        return Mono.just(new UsernamePasswordAuthenticationToken(null, token));
    }
}
