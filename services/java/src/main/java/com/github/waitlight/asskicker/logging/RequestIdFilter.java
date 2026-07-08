package com.github.waitlight.asskicker.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements WebFilter {
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = resolveRequestId(exchange.getRequest());
        exchange.getAttributes().put(REQUEST_ID_KEY, requestId);
        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(REQUEST_ID_KEY, requestId)) {
            log.debug("Incoming request {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getPath().value());
        }

        return chain.filter(exchange)
                .contextWrite(context -> context.put(REQUEST_ID_KEY, requestId));
    }

    private String resolveRequestId(ServerHttpRequest request) {
        String headerValue = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return headerValue;
    }
}
