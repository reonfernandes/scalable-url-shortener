package com.reon.apigateway.security;

import io.jsonwebtoken.Claims;
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

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthenticationFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final JwtService jwtService;
    private final TokenRevocationChecker revocationChecker;

    public AuthenticationFilter(JwtService jwtService, TokenRevocationChecker revocationChecker) {
        this.jwtService = jwtService;
        this.revocationChecker = revocationChecker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        Optional<String> jwtToken = jwtService.extractToken(request);
        if (jwtToken.isEmpty()) {
            log.warn("Gateway :: No token found for request: {}", request.getPath());
            return unauthorized(exchange.getResponse());
        }

        // Parse the token once and read every claim from the result.
        Optional<Claims> parsed = jwtService.parseClaims(jwtToken.get());
        if (parsed.isEmpty()) {
            log.warn("Gateway :: Invalid or expired token for path: {}", request.getPath());
            return unauthorized(exchange.getResponse());
        }
        Claims claims = parsed.get();

        return revocationChecker.isRevoked(claims).flatMap(revoked -> {
            if (revoked) {
                log.warn("Gateway :: Revoked token used for path: {}", request.getPath());
                return unauthorized(exchange.getResponse());
            }
            return authorize(exchange, chain, claims);
        });
    }

    private Mono<Void> authorize(ServerWebExchange exchange, GatewayFilterChain chain, Claims claims) {
        String userId = claims.get("userId", String.class);
        String roles = claims.get("roles", String.class);
        if (userId == null || roles == null) {
            log.warn("Gateway :: Token without userId or roles");
            return unauthorized(exchange.getResponse());
        }

        // Admin route protection
        String path = exchange.getRequest().getPath().value();
        boolean isAdmin = Arrays.asList(roles.split(",")).contains("ROLE_ADMIN");
        if (path.startsWith("/api/v1/admin") && !isAdmin) {
            log.warn("Gateway :: Access denied to admin route for userId: {}", userId);
            return forbidden(exchange.getResponse());
        }

        log.info("Gateway :: Authenticated userId: {}, roles: {}", userId, roles);

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
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
