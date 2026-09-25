package com.reon.apigateway.config;

import com.reon.apigateway.security.AuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class RouteConfig {
    private final AuthenticationFilter authenticationFilter;
    private final RedisRateLimiter passwordAttemptsRateLimiter;
    private final KeyResolver clientIpKeyResolver;

    public RouteConfig(AuthenticationFilter authenticationFilter, RedisRateLimiter passwordAttemptsRateLimiter,
                       KeyResolver clientIpKeyResolver) {
        this.authenticationFilter = authenticationFilter;
        this.passwordAttemptsRateLimiter = passwordAttemptsRateLimiter;
        this.clientIpKeyResolver = clientIpKeyResolver;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Password attempts are rate limited per IP (429 when over the limit)
                .route("user-service-auth", route -> route
                        .path("/api/v1/user/register", "/api/v1/user/login")
                        .filters(filter -> filter.requestRateLimiter(limit -> limit
                                .setRateLimiter(passwordAttemptsRateLimiter)
                                .setKeyResolver(clientIpKeyResolver)))
                        .uri("lb://user-service"))
                .route("user-service-logout", route -> route
                        .path("/api/v1/user/logout")
                        .uri("lb://user-service"))
                .route("user-service-admin", route -> route
                        .path("/api/v1/admin/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://user-service"))
                .route("user-service-protected", route -> route
                        .path("/api/v1/user/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://user-service"))
                // Unlocking a protected link = trying a password: rate limited like login
                .route("url-service-unlock", route -> route
                        .path("/api/v1/redirect/**").and().method(HttpMethod.POST)
                        .filters(filter -> filter.requestRateLimiter(limit -> limit
                                .setRateLimiter(passwordAttemptsRateLimiter)
                                .setKeyResolver(clientIpKeyResolver)))
                        .uri("lb://url-service"))
                .route("url-service-redirect", route -> route
                        .path("/api/v1/redirect/**")
                        .uri("lb://url-service"))
                .route("url-service-protected", route -> route
                        .path("/api/v1/url/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://url-service"))
                .route("analytics-service", route -> route
                        .path("/api/v1/analytics/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://analytics-service"))
                // short links like /abc123 → /api/v1/redirect/abc123 (keep this route last)
                .route("short-link", route -> route
                        .path("/{shortCode}").and().method(HttpMethod.GET)
                        .filters(filter -> filter.rewritePath("/(?<shortCode>.*)", "/api/v1/redirect/${shortCode}"))
                        .uri("lb://url-service"))
                .build();
    }
}
