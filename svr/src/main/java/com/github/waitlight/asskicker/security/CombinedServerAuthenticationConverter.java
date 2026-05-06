package com.github.waitlight.asskicker.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT token 形如 xxx.yyy.zzz（含两个点），API Key 形如 ak_xxx 或通过 X-API-Key 传递。
 * 按此规则将请求路由到对应的 Converter。
 */
public class CombinedServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String X_API_KEY = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtServerAuthenticationConverter jwtConverter = new JwtServerAuthenticationConverter();
    private final ApiKeyServerAuthenticationConverter apiKeyConverter = new ApiKeyServerAuthenticationConverter();

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

        if (looksLikeJwt(token)) {
            return jwtConverter.convert(exchange);
        }
        return apiKeyConverter.convert(exchange);
    }

    private boolean looksLikeJwt(String token) {
        int first = token.indexOf('.');
        if (first < 0) return false;
        int second = token.indexOf('.', first + 1);
        return second > first;
    }
}
