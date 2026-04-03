package com.reon.apigateway.config;

import com.reon.apigateway.security.AuthenticationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {
    private final AuthenticationFilter authenticationFilter;

    public RouteConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-public", route -> route
                        .path("/api/v1/user/register", "/api/v1/user/login", "/api/v1/user/verify-otp")
                        .uri("lb://user-service"))
                .route("user-service-admin", route -> route
                        .path("/api/v1/admin/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://user-service"))
                .route("user-service-protected", route -> route
                        .path("/api/v1/user/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://user-service"))
                .route("url-service-redirect", route -> route
                        .path("/api/v1/redirect/**")
                        .uri("lb://url-service"))
                .route("url-service-protected", route -> route
                        .path("/api/v1/url/**")
                        .filters(authFilter -> authFilter.filter(authenticationFilter))
                        .uri("lb://url-service"))
                .build();
    }
}
