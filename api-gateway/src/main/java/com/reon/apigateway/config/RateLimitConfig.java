package com.reon.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Limits how fast one IP can try passwords (login, sign-up, protected links).
 * Counters live in Redis, so the limit holds across gateway restarts and instances.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Each request costs 6 tokens and 1 token comes back per second, with room for 60:
     * that's 10 attempts at once, then 10 per minute. Over the limit the gateway answers 429.
     */
    @Bean
    public RedisRateLimiter passwordAttemptsRateLimiter() {
        return new RedisRateLimiter(1, 60, 6);
    }

    /** Rate limits are counted per client IP address. */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just(Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress)
                .map(address -> address.getHostAddress())
                .orElse("unknown"));
    }
}
