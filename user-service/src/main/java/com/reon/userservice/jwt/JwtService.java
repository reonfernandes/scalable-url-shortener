package com.reon.userservice.jwt;

import com.reon.userservice.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final String secretKey;
    private final Long expirationTime;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.jwt.expiration-time}") Long expirationTime,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.secretKey = secretKey;
        this.expirationTime = expirationTime;
        this.issuer = issuer;
    }

    public String generateToken(User user) {
        String jti = UUID.randomUUID().toString();
        String userId = user.getUserId();
        String email = user.getEmail();
        String roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("roles", roles);

        return Jwts.builder()
                .id(jti)
                .subject(email)
                .issuer(issuer)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key())
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
