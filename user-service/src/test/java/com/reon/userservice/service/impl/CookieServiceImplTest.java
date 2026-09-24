package com.reon.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieServiceImplTest {

    @Test
    void clearAccessTokenCookieExpiresTheCookie() {
        CookieServiceImpl cookieService = new CookieServiceImpl();
        ReflectionTestUtils.setField(cookieService, "cookieName", "accessToken");

        ResponseCookie cookie = cookieService.clearAccessTokenCookie();

        assertEquals("accessToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge().getSeconds());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
    }
}
