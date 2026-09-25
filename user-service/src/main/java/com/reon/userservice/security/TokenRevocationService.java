package com.reon.userservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * A JWT stays valid until it expires, so logging out or blocking a user needs a list of
 * cancelled tokens. It lives in Redis, and the API gateway checks it on every request.
 *
 * The key names must match TokenRevocationChecker in api-gateway.
 */
@Service
public class TokenRevocationService {
    /** One logged-out token, by its id (jti). */
    public static final String REVOKED_TOKEN_PREFIX = "auth:revoked-token:";
    /** Every token of a user issued at or before this time (epoch seconds) is cancelled. */
    public static final String REVOKED_BEFORE_PREFIX = "auth:revoked-before:";

    private final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);
    private final StringRedisTemplate redisTemplate;
    private final Duration tokenLifetime;

    public TokenRevocationService(StringRedisTemplate redisTemplate,
                                  @Value("${security.jwt.expiration-time}") long tokenLifetimeSeconds) {
        this.redisTemplate = redisTemplate;
        this.tokenLifetime = Duration.ofSeconds(tokenLifetimeSeconds);
    }

    /** Cancels one token (logout). Kept only until the token would have expired anyway. */
    public void revokeToken(String tokenId, Instant expiresAt) {
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (tokenId == null || remaining.isNegative() || remaining.isZero()) return;
        try {
            redisTemplate.opsForValue().set(REVOKED_TOKEN_PREFIX + tokenId, "1", remaining);
        } catch (RuntimeException exception) {
            log.error("Token Revocation :: Could not revoke token {}; it stays valid until it expires", tokenId, exception);
        }
    }

    /** Cancels every token the user has now (deactivation, account deletion). New logins still work. */
    public void revokeAllTokens(String userId) {
        try {
            redisTemplate.opsForValue().set(
                    REVOKED_BEFORE_PREFIX + userId,
                    String.valueOf(Instant.now().getEpochSecond()),
                    tokenLifetime);
        } catch (RuntimeException exception) {
            log.error("Token Revocation :: Could not revoke tokens of user {}; they stay valid until they expire", userId, exception);
        }
    }
}
