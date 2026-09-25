package com.reon.userservice.service.impl;

import com.reon.userservice.service.CookieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

@Service
public class CookieServiceImpl implements CookieService {
    @Value("${security.cookie.name}")
    private String cookieName;

    @Value("${security.cookie.expiration-time}")
    private Long cookieExpirationTime;

    private final Logger log = LoggerFactory.getLogger(CookieServiceImpl.class);

    @Override
    public ResponseCookie accessTokenCookie(String accessToken) {
        log.info("Cookie Service :: Generating cookie for access token...");
        ResponseCookie strict = ResponseCookie.from(cookieName, accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(cookieExpirationTime)
                .sameSite("Strict")
                .build();
        log.info("Cookie Service :: Cookie for access token generated");
        return strict;
    }

    // same cookie with maxAge 0 tells the browser to delete it
    @Override
    public ResponseCookie clearAccessTokenCookie() {
        log.info("Cookie Service :: Clearing access token cookie");
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    @Override
    public Optional<String> readAccessToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(value -> !value.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) return fromCookie;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }
}
