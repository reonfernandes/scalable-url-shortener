package com.reon.userservice.service;

import org.springframework.http.ResponseCookie;

public interface CookieService {
    ResponseCookie accessTokenCookie(String accessToken);
}