package com.reon.urlservice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;

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

    public String getJwtFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public Claims getClaimsFromToken(String token){
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return getClaimsFromToken(token)
                .get("userId")
                .toString();
    }

    public String getRoles(String token) {
        return getClaimsFromToken(token)
                .get("roles")
                .toString();
    }

    public String getTier(String token){
        return getClaimsFromToken(token)
                .get("tier")
                .toString();
    }

    public boolean isTokenValid(String token){
        try{
            getClaimsFromToken(token);
            return true;
        } catch (JwtException jwtException){
            return false;
        }
    }
}

