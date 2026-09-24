package com.reon.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Optional;


@Service
public class JwtService {
    private final String secretKey;
    private final String cookieName;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.cookie.name}") String cookieName
    ) {
        this.secretKey = secretKey;
        this.cookieName = cookieName;
    }
    
    public Optional<String> extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return Optional.of(cookie.getValue());
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Optional.of(authHeader.substring(7));
        }
        return Optional.empty();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String getUserId(String token) {
        return getClaims(token)
                .get("userId", String.class);
    }

    public String getRoles(String token) {
        return getClaims(token).get("roles", String.class);
    }
}
