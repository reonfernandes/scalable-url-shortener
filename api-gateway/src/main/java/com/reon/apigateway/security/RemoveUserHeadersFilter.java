package com.reon.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Services trust the X-User-* headers to know who is calling, so clients must never be able to send them.
 * This runs first on every route and removes them; AuthenticationFilter then adds the real values from the JWT.
 */
@Component
public class RemoveUserHeadersFilter implements GlobalFilter, Ordered {
    public static final List<String> USER_HEADERS = List.of("X-User-Id", "X-User-Roles");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> USER_HEADERS.forEach(headers::remove))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
