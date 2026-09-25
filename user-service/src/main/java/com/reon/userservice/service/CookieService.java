package com.reon.userservice.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

public interface CookieService {
    ResponseCookie accessTokenCookie(String accessToken);
    ResponseCookie clearAccessTokenCookie();
    /** The JWT sent with the request: the cookie first, then an "Authorization: Bearer" header. */
    Optional<String> readAccessToken(HttpServletRequest request);
}