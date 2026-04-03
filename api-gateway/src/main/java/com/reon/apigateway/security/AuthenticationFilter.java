package com.reon.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class AuthenticationFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final JwtService jwtService;

    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        Optional<String> jwtToken = jwtService.extractToken(request);
        return jwtToken.map(
                token -> authenticate(exchange, chain, token))
                .orElseGet(() -> {
                    log.warn("Gateway :: No token found for request: {}", request.getPath());
                    return unauthorized(exchange.getResponse());
                });
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        if (!jwtService.isTokenValid(token)) {
            log.warn("Gateway :: Invalid or expired token for path: {}", exchange.getRequest().getPath());
            return unauthorized(exchange.getResponse());
        }

        String userId = jwtService.getUserId(token);
        String roles = jwtService.getRoles(token);
        String tier = jwtService.getTier(token);

        // Admin route protection
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/api/v1/admin") && !roles.contains("ROLE_ADMIN")) {
            log.warn("Gateway :: Access denied to admin route for userId: {}", userId);
            return forbidden(exchange.getResponse());
        }

        log.info("Gateway :: Authenticated userId: {}, roles: {}", userId, roles);


        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .header("X-User-Tier", tier)
                .headers(headers -> headers.remove("Authorization"))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> forbidden(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }
}
