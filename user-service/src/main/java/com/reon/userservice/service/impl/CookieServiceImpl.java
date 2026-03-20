package com.reon.userservice.service.impl;

import com.reon.userservice.service.CookieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieServiceImpl implements CookieService {
    @Value("${security.cookie.name}")
    private String cookieName;

    @Value("${security.cookie.expiration-time}")
    private Long cookieExpirationTime;

    private final Logger log = LoggerFactory.getLogger(CookieServiceImpl.class);

    @Override
    public ResponseCookie accessTokenCookie(String accessToken) {
        log.info("Generating cookie for access token...");
        return ResponseCookie.from(cookieName, accessToken)
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(cookieExpirationTime)
                .sameSite("Strict")
                .build();
    }
}