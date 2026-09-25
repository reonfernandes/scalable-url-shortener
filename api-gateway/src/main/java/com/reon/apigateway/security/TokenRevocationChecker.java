package com.reon.apigateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Checks the list of cancelled tokens that user-service writes to Redis on logout,
 * deactivation and account deletion. The key names must match TokenRevocationService there.
 */
@Component
public class TokenRevocationChecker {
    private static final String REVOKED_TOKEN_PREFIX = "auth:revoked-token:";
    private static final String REVOKED_BEFORE_PREFIX = "auth:revoked-before:";

    private final Logger log = LoggerFactory.getLogger(TokenRevocationChecker.class);
    private final ReactiveStringRedisTemplate redisTemplate;

    public TokenRevocationChecker(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isRevoked(Claims claims) {
        String userId = claims.get("userId", String.class);
        long issuedAt = claims.getIssuedAt() == null ? 0 : claims.getIssuedAt().toInstant().getEpochSecond();

        Mono<Boolean> tokenLoggedOut = claims.getId() == null
                ? Mono.just(false)
                : redisTemplate.hasKey(REVOKED_TOKEN_PREFIX + claims.getId());

        // Tokens issued at or before this time were cancelled (user deactivated or deleted).
        Mono<Boolean> userTokensRevoked = redisTemplate.opsForValue()
                .get(REVOKED_BEFORE_PREFIX + userId)
                .map(revokedBefore -> issuedAt <= Long.parseLong(revokedBefore))
                .defaultIfEmpty(false);

        return Mono.zip(tokenLoggedOut, userTokensRevoked, (loggedOut, revoked) -> loggedOut || revoked)
                .onErrorResume(error -> {
                    // If Redis is down, don't lock every user out; the token still expires on time.
                    log.warn("Gateway :: Could not check revoked tokens, allowing request: {}", error.getMessage());
                    return Mono.just(false);
                });
    }
}
