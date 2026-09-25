package com.reon.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds a shared secret to every request the gateway forwards. The services reject requests
 * without it (GatewayOnlyFilter in common-lib), so nobody can reach a service directly and
 * send a fake X-User-Id. Any value a client sends in this header is overwritten.
 */
@Component
public class AddGatewaySecretFilter implements GlobalFilter, Ordered {
    public static final String HEADER = "X-Gateway-Secret";

    private final String gatewaySecret;

    public AddGatewaySecretFilter(@Value("${security.gateway.secret}") String gatewaySecret) {
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(HEADER, gatewaySecret))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        // right after RemoveUserHeadersFilter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
